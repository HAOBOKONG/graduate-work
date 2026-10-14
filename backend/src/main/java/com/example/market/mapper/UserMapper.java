package com.example.market.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.market.entity.User;
import org.apache.ibatis.annotations.Mapper;

/** 基础用户数据访问；不提供注册、登录或权限功能。 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
