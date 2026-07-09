# PRD Input Template

Use this template when providing a feature specification. Fill in as much detail as possible
for the best generation quality. Fields marked [Required] must be filled.

## 1. Project Meta [Required]

```yaml
project-name: "Order Management System"
description: "Manage customer orders, track status, and generate reports"
base-package: "com.example.order"
```

## 2. Functional Modules [Required]

For each module, list:

```yaml
modules:
  - name: "Order Management"           # [Required]
    path:  "/order-manage"              # [Required] kebab-case, will be route + controller prefix
    description: "CRUD for orders"
    features:
      - type: "page"                   # page | add | edit | delete | export
      - type: "add"
      - type: "edit"
      - type: "delete"
    fields:                            # [Required] Database columns
      - name: "id"                     # [Auto] skip - auto-generated
        type: "Long"
      - name: "orderNo"
        type: "String"
        label: "Order Number"
        length: 32
        nullable: false
        searchable: true              # if true, add query condition
      - name: "customerName"
        type: "String"
        label: "Customer Name"
        length: 64
        nullable: false
      - name: "amount"
        type: "BigDecimal"
        label: "Order Amount"
        precision: 10
        scale: 2
        nullable: false
      - name: "status"
        type: "String"
        label: "Status"
        length: 20
        nullable: false
        defaultValue: "PENDING"

  - name: "Product Management"
    path:  "/product-manage"
    description: "CRUD for products"
    # ...
```

## 3. Business Rules (Optional)

Describe any special validation or logic:

```
- Order amount must be > 0
- When deleting an order, check if status is PENDING
- customerName max 64 characters
```

## 4. Relationships (Optional)

If entities relate to each other:

```
- Order.customerId -> Customer.id
- Order contains multiple OrderItem -> order_id FK
```

## 5. PRD Minimal Example

Even with minimal input like "I need a user management page with name, email, and phone fields",
the skill will expand it into the full format above. Provide at minimum:

1. Module name
2. Field names and types
3. What CRUD operations are needed
