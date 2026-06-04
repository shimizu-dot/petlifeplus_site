package com.example.petlife.mapper;

import com.example.petlife.dto.pet.PetCareContextRow;
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
        SELECT id, owner_user_id, name, species, breed, sex, birth_date, weight_baseline_kg, image_path,
               deceased_at, deleted_at, created_at, updated_at
        FROM pets WHERE deleted_at IS NULL ORDER BY id
        LIMIT #{limit} OFFSET #{offset}
        """)
    List<PetEntity> findAll(@Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM pets WHERE deleted_at IS NULL")
    long countAll();

    // ---- オーナー別（一般ユーザー用） ----
    @Select("""
        SELECT id, owner_user_id, name, species, breed, sex, birth_date, weight_baseline_kg, image_path,
               deceased_at, deleted_at, created_at, updated_at
        FROM pets WHERE owner_user_id = #{ownerUserId} AND deleted_at IS NULL ORDER BY id
        LIMIT #{limit} OFFSET #{offset}
        """)
    List<PetEntity> findByOwnerUserId(@Param("ownerUserId") Long ownerUserId,
                                       @Param("limit") int limit,
                                       @Param("offset") int offset);

    @Select("""
        SELECT id, owner_user_id, name, species, breed, sex, birth_date, weight_baseline_kg, image_path,
               deceased_at, deleted_at, created_at, updated_at
        FROM pets WHERE owner_user_id = #{ownerUserId} AND deleted_at IS NULL ORDER BY id
        """)
    List<PetEntity> findActiveByOwnerUserId(@Param("ownerUserId") Long ownerUserId);

    @Select("SELECT COUNT(*) FROM pets WHERE owner_user_id = #{ownerUserId} AND deleted_at IS NULL")
    long countByOwnerUserId(@Param("ownerUserId") Long ownerUserId);

    // ---- 単件取得 ----
    @Select("""
        SELECT id, owner_user_id, name, species, breed, sex, birth_date, weight_baseline_kg, image_path,
               deceased_at, deleted_at, created_at, updated_at
        FROM pets WHERE id = #{id} AND deleted_at IS NULL
        """)
    PetEntity findById(@Param("id") Long id);

    @Select("""
        SELECT id, owner_user_id, name, species, breed, sex, birth_date, weight_baseline_kg, image_path,
               deceased_at, deleted_at, created_at, updated_at
        FROM pets WHERE id = #{id} AND owner_user_id = #{ownerUserId} AND deleted_at IS NULL
        """)
    PetEntity findByIdAndOwnerUserId(@Param("id") Long id, @Param("ownerUserId") Long ownerUserId);

    // ---- 更新系 ----
    // INSERT...RETURNING は結果セットを返すため @Select を使用（@Insert では Long 戻り値に写像されない）
    @Select("""
        INSERT INTO pets(owner_user_id, name, species, breed, sex, birth_date, weight_baseline_kg, image_path,
                         deceased_at, created_at, updated_at)
        VALUES(#{ownerUserId}, #{name}, #{species}, #{breed}, #{sex}, #{birthDate},
               #{weightBaselineKg}, #{imagePath}, #{deceasedAt}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        RETURNING id
        """)
    Long insertReturningId(PetEntity pet);

    @Update("""
        UPDATE pets
        SET name = #{name}, species = #{species}, breed = #{breed}, sex = #{sex},
            birth_date = #{birthDate}, weight_baseline_kg = #{weightBaselineKg},
            image_path = #{imagePath}, deceased_at = #{deceasedAt},
            updated_at = CURRENT_TIMESTAMP
        WHERE id = #{id} AND deleted_at IS NULL
        """)
    int update(PetEntity pet);

    @Update("""
        UPDATE pets SET deleted_at = #{deletedAt}, updated_at = CURRENT_TIMESTAMP
        WHERE id = #{id} AND deleted_at IS NULL
        """)
    int softDelete(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);

    @Select("""
        SELECT
          (CASE WHEN EXISTS (SELECT 1 FROM health_records h WHERE h.pet_id = #{petId} AND h.deleted_at IS NULL) THEN 1 ELSE 0 END) +
          (CASE WHEN EXISTS (SELECT 1 FROM pet_care_records c WHERE c.pet_id = #{petId} AND c.deleted_at IS NULL) THEN 1 ELSE 0 END) +
          (CASE WHEN EXISTS (SELECT 1 FROM symptom_checks s WHERE s.pet_id = #{petId}) THEN 1 ELSE 0 END) +
          (CASE WHEN EXISTS (SELECT 1 FROM appointments a WHERE a.pet_id = #{petId} AND a.deleted_at IS NULL) THEN 1 ELSE 0 END) +
          (CASE WHEN EXISTS (SELECT 1 FROM medical_histories m WHERE m.pet_id = #{petId} AND m.deleted_at IS NULL) THEN 1 ELSE 0 END)
        """)
    int countLinkedDataFlags(@Param("petId") Long petId);

    @Select("""
        SELECT p.id AS "petId",
               p.owner_user_id AS "ownerUserId",
               u.name AS "ownerName",
               UPPER(pl.name) AS "planName"
        FROM pets p
        JOIN users u ON u.id = p.owner_user_id
        LEFT JOIN subscriptions s
               ON s.user_id = p.owner_user_id
              AND s.deleted_at IS NULL
              AND s.status = 'ACTIVE'
              AND s.start_date <= CURRENT_DATE
              AND (s.end_date IS NULL OR s.end_date >= CURRENT_DATE)
        LEFT JOIN plans pl
               ON pl.id = s.plan_id
              AND pl.deleted_at IS NULL
              AND pl.is_active = TRUE
        WHERE p.id = #{petId}
          AND p.deleted_at IS NULL
        ORDER BY s.start_date DESC NULLS LAST, s.id DESC NULLS LAST
        LIMIT 1
        """)
    PetCareContextRow findCareContextByPetId(@Param("petId") Long petId);

    @Select("""
        SELECT sibling.name
        FROM pets base
        JOIN pets sibling
          ON sibling.owner_user_id = base.owner_user_id
         AND sibling.deleted_at IS NULL
         AND sibling.id <> base.id
        WHERE base.id = #{petId}
          AND base.deleted_at IS NULL
        ORDER BY sibling.id
        """)
    List<String> findSiblingNamesByPetId(@Param("petId") Long petId);

    @Update("""
        UPDATE pets
        SET deceased_at = COALESCE(deceased_at, #{deceasedAt}), updated_at = CURRENT_TIMESTAMP
        WHERE id = #{id} AND deleted_at IS NULL
        """)
    int markDeceased(@Param("id") Long id, @Param("deceasedAt") LocalDateTime deceasedAt);
}
