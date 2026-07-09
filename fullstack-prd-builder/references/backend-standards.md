# Backend Coding Standards (Spring Boot)

These standards apply regardless of whether MyBatis-Plus or JPA is chosen.

## 1. Layered Parameter Passing (MANDATORY)

### Controller -> DTO

Controllers receive request data via **DTO** classes. Never expose Entity directly in controller parameters.

```java
// CORRECT: Use DTO
@PostMapping
public Result<Void> save(@RequestBody @Valid UserSaveDTO dto) { ... }

@GetMapping("/page")
public Result<IPage<UserVO>> page(UserQueryDTO dto) { ... }

// WRONG: Do NOT use Entity directly
@PostMapping
public Result<Void> save(@RequestBody User entity) { ... }  // FORBIDDEN
```

### Service -> VO

Service methods return **VO** classes to the controller. Never return Entity to the controller layer.

```java
// CORRECT: Service returns VO
public interface UserService {
    IPage<UserVO> page(UserQueryDTO dto);
    UserVO getById(Long id);
    void save(UserSaveDTO dto);
}

// WRONG: Service returning Entity
public interface UserService {
    List<User> list();  // FORBIDDEN
}
```

### Vice-Versa Rules

- **Controller -> Service**: DTO
- **Service -> Controller**: VO
- **Service -> Persistence Layer**: Entity
- **Entity <-> DTO/VO**: Convert via helper (manual setter or MapStruct)

## 2. Package Structure

```
{basePackage}/
  controller/     # @RestController
  service/        # Interface
  service/impl/   # @Service implementation
  mapper/         # MyBatis-Plus: extends BaseMapper<Entity>
  repository/     # JPA: extends JpaRepository<Entity, Long>
  entity/         # extends BaseEntity, @TableName (MP) / @Entity (JPA)
  dto/            # XxxSaveDTO, XxxQueryDTO, etc.
  vo/             # XxxVO, PageResultVO
  config/         # MybatisPlusConfig / JpaAuditConfig / CorsConfig
  exception/      # GlobalExceptionHandler, BusinessException
```

## 3. Exception Handling (MANDATORY)

Every project must include `GlobalExceptionHandler`:


## 4. BaseEntity (MANDATORY)

Every entity must extend a base class containing the 4 audit fields.
The implementation differs by persistence framework:

### MyBatis-Plus

```java
package {basePackage}.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public abstract class BaseEntity {

    @TableField(fill = FieldFill.INSERT)
    private String createUser;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateUser;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
```

Also create a `MetaObjectHandler` config to auto-fill these fields.

### JPA

```java
package {basePackage}.entity;

import lombok.Data;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
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
```

Also create a `JpaAuditConfig` with `@EnableJpaAuditing` and an `AuditorAware<String>` bean.

## 5. Controller Path Alignment

Controller `@RequestMapping` paths **must match** the API paths defined in the PRD's API documentation:

```
PRD API doc:  GET  /api/user-manage/page  ->  @GetMapping("/page") on @RequestMapping("/api/user-manage")
PRD API doc:  POST /api/user-manage       ->  @PostMapping on @RequestMapping("/api/user-manage")
```

Every project must include `GlobalExceptionHandler` that catches the following exception types
(see `assets/backend-template/GlobalExceptionHandler.java` for the full annotated implementation):

### 400 Bad Request — Client input / validation errors

| Exception | Trigger |
|-----------|---------|
| `MethodArgumentNotValidException` | `@Valid` on `@RequestBody` DTO fails |
| `BindException` | `@Valid` on a form-bind target |
| `ConstraintViolationException` | `@Validated` on path/query params |
| `HttpMessageNotReadableException` | Missing/malformed JSON request body |
| `MissingServletRequestParameterException` | Required query param is absent |
| `TypeMismatchException` | Type conversion fails (e.g., "abc" -> Long) |
| `IllegalArgumentException` | Invalid method argument |

### 403 Forbidden

| Exception | Trigger |
|-----------|---------|
| `AccessDeniedException` | Spring Security — no permission / role mismatch |

### 404 Not Found

| Exception | Trigger |
|-----------|---------|
| `NoHandlerFoundException` | No handler mapped for the requested URL |
| `NoResourceFoundException` (JDK 17 / SB 3.x only) | Static resource not found |

### 405 / 415 — HTTP method / media type

| Exception | Trigger |
|-----------|---------|
| `HttpRequestMethodNotSupportedException` | Wrong HTTP method (GET on POST-only endpoint) |
| `HttpMediaTypeNotSupportedException` | Wrong Content-Type header |

### 409 Conflict — Data integrity

| Exception | Trigger |
|-----------|---------|
| `DuplicateKeyException` | Unique index / primary key violation |
| `DataIntegrityViolationException` | Any DB constraint violation (not null, check, FK, etc.) |

### 500 Internal Server Error — Server-side failures

| Exception | Trigger |
|-----------|---------|
| `HttpMessageNotWritableException` | Failed to serialize response to JSON |
| `RuntimeException` | Generic unanticipated runtime errors |
| `Exception` | Ultimate catch-all for anything else |

All handlers use `@ResponseStatus` to set the correct HTTP status code and `log.warn`/`log.error`
for observability. The catch-all `Exception` handler masks internal details from the client
(returns a generic message), while the `RuntimeException` handler exposes the message for
developer convenience in non-production environments.
