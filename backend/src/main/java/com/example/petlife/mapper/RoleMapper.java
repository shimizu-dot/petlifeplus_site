package com.example.petlife.mapper;

import com.example.petlife.entity.RoleEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface RoleMapper {

    @Select("SELECT id, role_code, role_name, created_at, updated_at FROM roles ORDER BY id")
    List<RoleEntity> findAll();

    @Select("SELECT id, role_code, role_name, created_at, updated_at FROM roles WHERE id = #{id}")
    RoleEntity findById(@Param("id") Long id);

    @Select("SELECT id, role_code, role_name, created_at, updated_at FROM roles WHERE role_code = #{roleCode}")
    RoleEntity findByCode(@Param("roleCode") String roleCode);

    // INSERT...RETURNING は結果セットを返すため @Select を使用（@Insert では Long 戻り値に写像されない）
    @Select("""
        INSERT INTO roles(role_code, role_name, created_at, updated_at)
        VALUES(#{roleCode}, #{roleName}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        RETURNING id
        """)
    Long insertReturningId(RoleEntity role);

    @Update("""
        UPDATE roles
        SET role_code = #{roleCode}, role_name = #{roleName}, updated_at = CURRENT_TIMESTAMP
        WHERE id = #{id}
        """)
    int update(RoleEntity role);
}
