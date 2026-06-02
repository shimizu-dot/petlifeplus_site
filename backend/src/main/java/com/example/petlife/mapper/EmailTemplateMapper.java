package com.example.petlife.mapper;

import com.example.petlife.entity.EmailTemplateEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface EmailTemplateMapper {

    @Select("""
        SELECT id, template_code, subject_template, body_template, is_active, created_at, updated_at
        FROM email_templates
        WHERE is_active = TRUE
        ORDER BY template_code
        """)
    List<EmailTemplateEntity> findAllActive();

    @Select("""
        SELECT id, template_code, subject_template, body_template, is_active, created_at, updated_at
        FROM email_templates WHERE id = #{id}
        """)
    EmailTemplateEntity findById(@Param("id") Long id);

    @Select("""
        SELECT id, template_code, subject_template, body_template, is_active, created_at, updated_at
        FROM email_templates WHERE template_code = #{templateCode}
        """)
    EmailTemplateEntity findByCode(@Param("templateCode") String templateCode);

    // INSERT...RETURNING は結果セットを返すため @Select を使用（@Insert では Long 戻り値に写像されない）
    @Select("""
        INSERT INTO email_templates(template_code, subject_template, body_template, is_active, created_at, updated_at)
        VALUES(#{templateCode}, #{subjectTemplate}, #{bodyTemplate}, #{isActive}, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        RETURNING id
        """)
    Long insertReturningId(EmailTemplateEntity template);

    @Update("""
        UPDATE email_templates
        SET subject_template = #{subjectTemplate}, body_template = #{bodyTemplate},
            is_active = #{isActive}, updated_at = CURRENT_TIMESTAMP
        WHERE id = #{id}
        """)
    int update(EmailTemplateEntity template);
}
