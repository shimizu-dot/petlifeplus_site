package com.example.petlife.mapper;

import com.example.petlife.entity.PetEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PetMapper {

    // ---- 全件（管理者用） ----
    @Select("""
        SELECT id, owner_user_id, name, species, breed, sex, birth_date, weight_baseline_kg,
               deleted_at, created_at, updated_at
        FROM pets WHERE deleted_at IS NULL ORDER BY id
        LIMIT #{limit} OFFSET #{offset}
        """)
    List<PetEntity> findAll(@Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM pets WHERE deleted_at IS NULL")
    long countAll();

    // ---- オーナー別（一般ユーザー用） ----
    @Select("""
        SELECT id, owner_user_id, name, species, breed, sex, birth_date, weight_baseline_kg,
               deleted_at, created_at, updated_at
        FROM pets WHERE owner_user_id = #{ownerUserId} AND deleted_at IS NULL ORDER BY id
        LIMIT #{limit} OFFSET #{offset}
        """)
    List<PetEntity> findByOwnerUserId(@Param("ownerUserId") Long ownerUserId,
                                       @Param("limit") int limit,
                                       @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM pets WHERE owner_user_id = #{ownerUserId} AND deleted_at IS NULL")
    long countByOwnerUserId(@Param("ownerUserId") Long ownerUserId);

    // ---- 単件取得 ----
    @Select("""
        SELECT id, owner_user_id, name, species, breed, sex, birth_date, weight_baseline_kg,
               deleted_at, created_at, updated_at
        FROM pets WHERE id = #{id} AND deleted_at IS NULL
        """)
    PetEntity findById(@Param("id") Long id);

    @Select("""
        SELECT id, owner_user_id, name, species, breed, sex, birth_date, weight_baseline_kg,
               deleted_at, created_at, updated_at
        FROM pets WHERE id = #{id} AND owner_user_id = #{ownerUserId} AND deleted_at IS NULL
        """)
    PetEntity findByIdAndOwnerUserId(@Param("id") Long id, @Param("ownerUserId") Long ownerUserId);

    // ---- 更新系 ----
    @Select("""
        INSERT INTO pets(owner_user_id, name, species, breed, sex, birth_date, weight_baseline_kg,
                         created_at, updated_at)
        VALUES(#{ownerUserId}, #{name}, #{species}, #{breed}, #{sex}, #{birthDate},
               #{weightBaselineKg}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        RETURNING id
        """)
    Long insertReturningId(PetEntity pet);

    @Update("""
        UPDATE pets
        SET name = #{name}, species = #{species}, breed = #{breed}, sex = #{sex},
            birth_date = #{birthDate}, weight_baseline_kg = #{weightBaselineKg},
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{id} AND deleted_at IS NULL
        """)
    int update(PetEntity pet);

    @Update("""
        UPDATE pets SET deleted_at = #{deletedAt}, updated_at = CURRENT_TIMESTAMP
        WHERE id = #{id} AND deleted_at IS NULL
        """)
    int softDelete(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);
}
