package com.example.trainticketoffice.model;


import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.time.ZoneId;

@MappedSuperclass
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public abstract class BaseEntity {
    @CreationTimestamp  // Hibernate sẽ auto set timestamp khi persist/update, không cần @PrePersist.
    @Column(name = "created_at" , nullable = false, updatable = false)
    OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate(){
        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")); // set giờ việt nam UTC + 7
        createdAt = now;
    }

}
