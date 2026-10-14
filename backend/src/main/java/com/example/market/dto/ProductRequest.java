package com.example.market.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
/** 不接受sellerId或status；归属和状态始终由服务端决定。 */
public record ProductRequest(
    @NotBlank(message="标题不能为空") @Size(max=200, message="标题最多200字") String title,
    @NotBlank(message="描述不能为空") @Size(max=10000, message="描述最多10000字") String description,
    @NotNull(message="请选择分类") @Positive(message="分类不合法") Long categoryId,
    @NotNull(message="请输入价格") @DecimalMin(value="0.01", message="价格必须大于0")
    @Digits(integer=10, fraction=2, message="价格最多10位整数和2位小数") BigDecimal price,
    @DecimalMin(value="0", message="原价不能为负数")
    @Digits(integer=10, fraction=2, message="原价最多10位整数和2位小数") BigDecimal originalPrice,
    @NotNull(message="请选择成色") @Min(value=1, message="成色为1至5级") @Max(value=5, message="成色为1至5级") Integer conditionLevel,
    @Size(max=8, message="最多8张图片") List<@NotBlank @Size(max=1024) String> imageUrls
) {}
