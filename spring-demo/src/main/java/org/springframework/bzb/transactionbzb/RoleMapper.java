package org.springframework.bzb.transactionbzb;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RoleMapper {
    @Select("SELECT * FROM role WHERE id = #{id}")
    Role getById(int id);

	@Insert("INSERT INTO role (id, role) VALUES (#{id}, #{role})")
	int insert(Role role);
}