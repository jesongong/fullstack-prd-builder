# Database Coding Standards (MySQL)

## 1. Naming Conventions (MANDATORY)

All table and column names use **snake_case** (lowercase, words separated by underscores).

```
Table:  user_manage, order_detail, sys_config
Column: user_name, create_time, order_status
```

## 2. Mandatory Audit Fields (MANDATORY)

Every business table **must** include these 4 fields:

| Column       | Type          | Description    |
|-------------|---------------|----------------|
| CREATE_USER | VARCHAR(64)   | Created by     |
| CREATE_TIME | DATETIME      | Created at     |
| UPDATE_USER | VARCHAR(64)   | Updated by     |
| UPDATE_TIME | DATETIME      | Updated at     |

Full DDL snippet for every table:

```sql
CREATE_TABLE {prefix}{table_name} (
    id          BIGINT       NOT NULL AUTO_INCREMENT  COMMENT 'Primary key',
    -- business columns here --

    CREATE_USER VARCHAR(64)  DEFAULT NULL             COMMENT 'Created by',
    CREATE_TIME DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
    UPDATE_USER VARCHAR(64)  DEFAULT NULL             COMMENT 'Updated by',
    UPDATE_TIME DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Table description';
```

## 3. Table Prefix

All table names must use the prefix confirmed by the user in the database clarification step (Step 2 of the workflow).

Examples with prefix `sys_`:
- `sys_user`
- `sys_role`
- `sys_user_role`

## 4. Entity Mapping

Entity classes map to the full table name including prefix. The annotation differs by framework:

**MyBatis-Plus** (use `@TableName`):

```java
@Data
@TableName("{prefix}user")
public class User extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    // audit fields inherited from BaseEntity
}
```

**JPA** (use `@Table`):

```java
@Data
@Entity
@Table(name = "{prefix}user")
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    // audit fields inherited from BaseEntity
}
```
