package {{basePackage}}.entity;

import lombok.Data;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;

@Data
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @CreatedBy
    @Column(name = "CREATE_USER", length = 64, updatable = false)
    private String createUser;

    @CreatedDate
    @Column(name = "CREATE_TIME", updatable = false)
    private LocalDateTime createTime;

    @LastModifiedBy
    @Column(name = "UPDATE_USER", length = 64)
    private String updateUser;

    @LastModifiedDate
    @Column(name = "UPDATE_TIME")
    private LocalDateTime updateTime;
}
