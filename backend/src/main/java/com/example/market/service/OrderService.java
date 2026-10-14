package com.example.market.service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.market.entity.Order;
import com.example.market.entity.Product;
import com.example.market.entity.User;
import com.example.market.mapper.*;
import com.example.market.vo.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderService {
    private final OrderMapper orders;
    private final ProductMapper products;
    private final UserMapper users;
    public OrderService(OrderMapper orders, ProductMapper products, UserMapper users) {
        this.orders=orders; this.products=products; this.users=users;
    }
    @Transactional
    public OrderVO create(Long productId, Long buyerId) {
        Product product=products.selectById(productId);
        if (product==null) throw fail(HttpStatus.NOT_FOUND,"商品不存在");
        if (Objects.equals(product.getSellerId(),buyerId)) throw fail(HttpStatus.BAD_REQUEST,"不能购买自己发布的商品");
        // 条件UPDATE取得行锁，竞争购买只有一个请求成功；订单写入失败则预留自动回滚。
        int reserved=products.update(null,new UpdateWrapper<Product>().eq("id",productId).eq("status","ON_SALE").set("status","RESERVED"));
        if (reserved!=1) throw fail(HttpStatus.CONFLICT,"商品已被购买或已不在售");
        // 再读取锁定后的商品，保存本次成交的标题和价格快照。
        product=products.selectById(productId);
        Order order=new Order();
        order.setOrderNo("ORD"+UUID.randomUUID().toString().replace("-","").toUpperCase(Locale.ROOT));
        order.setBuyerId(buyerId); order.setSellerId(product.getSellerId()); order.setProductId(productId);
        order.setProductTitleSnapshot(product.getTitle()); order.setPriceSnapshot(product.getPrice()); order.setStatus("WAIT_PAY");
        try { orders.insert(order); }
        catch (DuplicateKeyException exception) { throw fail(HttpStatus.CONFLICT,"该商品已有有效订单，请刷新后重试"); }
        return detail(order.getId(),buyerId);
    }
    public MyOrdersVO my(Long userId) {
        List<Order> rows=orders.selectList(new QueryWrapper<Order>().and(q -> q.eq("buyer_id",userId).or().eq("seller_id",userId))
            .orderByDesc("created_at","id"));
        Map<Long,User> people=people(rows);
        return new MyOrdersVO(rows.stream().filter(o -> Objects.equals(o.getBuyerId(),userId)).map(o -> view(o,people)).toList(),
            rows.stream().filter(o -> Objects.equals(o.getSellerId(),userId)).map(o -> view(o,people)).toList());
    }
    public OrderVO detail(Long id, Long userId) {
        Order order=find(id);
        if (!Objects.equals(order.getBuyerId(),userId) && !Objects.equals(order.getSellerId(),userId)) {
            throw fail(HttpStatus.FORBIDDEN,"只能查看自己参与的订单");
        }
        return view(order,people(List.of(order)));
    }
    @Transactional
    public OrderVO pay(Long id, Long userId) {
        transition(id,userId,"buyer_id","WAIT_PAY","PAID");
        return detail(id,userId);
    }
    @Transactional
    public OrderVO ship(Long id, Long userId) {
        transition(id,userId,"seller_id","PAID","SHIPPED");
        return detail(id,userId);
    }
    @Transactional
    public OrderVO complete(Long id, Long userId) {
        Order order=transition(id,userId,"buyer_id","SHIPPED","COMPLETED");
        int sold=products.update(null,new UpdateWrapper<Product>().eq("id",order.getProductId())
            .eq("seller_id",order.getSellerId()).eq("status","RESERVED").set("status","SOLD"));
        if (sold!=1) throw fail(HttpStatus.CONFLICT,"商品状态异常，无法完成交易");
        return detail(id,userId);
    }
    private Order transition(Long id, Long userId, String actor, String from, String to) {
        Order order=find(id);
        Long expected="buyer_id".equals(actor)?order.getBuyerId():order.getSellerId();
        if (!Objects.equals(expected,userId)) throw fail(HttpStatus.FORBIDDEN,
            "buyer_id".equals(actor)?"只有订单买家可以执行此操作":"只有订单卖家可以发货");
        int changed=orders.update(null,new UpdateWrapper<Order>().eq("id",id).eq(actor,userId).eq("status",from).set("status",to));
        if (changed!=1) throw fail(HttpStatus.CONFLICT,"订单状态不允许此操作，请刷新后重试");
        return order;
    }
    private Order find(Long id) {
        Order order=orders.selectById(id);
        if(order==null) throw fail(HttpStatus.NOT_FOUND,"订单不存在");
        return order;
    }
    private Map<Long,User> people(List<Order> rows) {
        if(rows.isEmpty()) return Map.of();
        Set<Long> ids=new HashSet<>(); rows.forEach(o -> { ids.add(o.getBuyerId()); ids.add(o.getSellerId()); });
        return users.selectList(new QueryWrapper<User>().in("id",ids)).stream().collect(Collectors.toMap(User::getId,u -> u));
    }
    private OrderVO view(Order o, Map<Long,User> people) {
        User buyer=people.get(o.getBuyerId()), seller=people.get(o.getSellerId());
        return new OrderVO(o.getId(),o.getOrderNo(),o.getBuyerId(),o.getSellerId(),o.getProductId(),o.getProductTitleSnapshot(),
            o.getPriceSnapshot(),o.getStatus(),o.getCreatedAt(),o.getUpdatedAt(),
            new OrderVO.OrderUserVO(buyer.getId(),buyer.getUsername(),buyer.getNickname()),
            new OrderVO.OrderUserVO(seller.getId(),seller.getUsername(),seller.getNickname()));
    }
    private ResponseStatusException fail(HttpStatus status,String message) { return new ResponseStatusException(status,message); }
}
