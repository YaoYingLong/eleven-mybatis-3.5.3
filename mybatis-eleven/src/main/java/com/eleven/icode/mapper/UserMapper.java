package com.eleven.icode.mapper;

import com.eleven.icode.entity.User;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface UserMapper {
    User selectById(Long param1, String table);

    @Select(value = "select * from t_user")
    List<User> selectAllUser();
}
