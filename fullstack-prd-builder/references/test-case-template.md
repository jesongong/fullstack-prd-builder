# Test Case Template

Generate all test classes following this template. Each test file must be self-contained,
independent, and order-agnostic.

---

## Backend Test Template (JUnit 5 + Mockito + AssertJ)

### Controller Test: `{EntityName}ControllerTest.java`

```java
package {{basePackage}}.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import {{basePackage}}.common.Result;
import {{basePackage}}.dto.{{EntityName}}SaveDTO;
import {{basePackage}}.dto.{{EntityName}}QueryDTO;
import {{basePackage}}.vo.{{EntityName}}VO;
import {{basePackage}}.vo.PageResultVO;
import {{basePackage}}.service.{{EntityName}}Service;
import {{basePackage}}.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({{EntityName}}Controller.class)
@DisplayName("{{EntityName}} Controller")
class {{EntityName}}ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private {{EntityName}}Service {{entityName}}Service;

    // ==================== Happy Path ====================

    @Nested
    @DisplayName("POST /api/{{entityName}}")
    class Save {

        @Test
        @DisplayName("Valid request -> 200 + returned VO")
        void save_ValidRequest_ShouldReturnOk() throws Exception {
            {{EntityName}}SaveDTO dto = buildValidSaveDTO();
            {{EntityName}}VO vo = buildVO(1L);
            when({{entityName}}Service.save(any())).thenReturn(vo);

            mockMvc.perform(post("/api/{{entityName}}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.id").value(1L));

            verify({{entityName}}Service).save(any());
        }
    }

    @Nested
    @DisplayName("GET /api/{{entityName}}/page")
    class PageQuery {

        @Test
        @DisplayName("Default pagination -> 200 + pageResult")
        void page_DefaultParams_ShouldReturnPage() throws Exception {
            PageResultVO<{{EntityName}}VO> page = new PageResultVO<>();
            page.setRecords(Collections.emptyList());
            page.setTotal(0L);
            when({{entityName}}Service.page(any())).thenReturn(page);

            mockMvc.perform(get("/api/{{entityName}}/page")
                    .param("page", "1")
                    .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.total").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/{{entityName}}/{id}")
    class GetById {

        @Test
        @DisplayName("Existing id -> 200 + VO")
        void getById_Existing_ShouldReturnVO() throws Exception {
            {{EntityName}}VO vo = buildVO(1L);
            when({{entityName}}Service.getById(1L)).thenReturn(vo);

            mockMvc.perform(get("/api/{{entityName}}/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(1L));
        }

        @Test
        @DisplayName("Non-existing id -> 404")
        void getById_NotExists_ShouldReturn404() throws Exception {
            when({{entityName}}Service.getById(999L))
                    .thenThrow(new RuntimeException("Not found"));

            mockMvc.perform(get("/api/{{entityName}}/999"))
                    .andExpect(status().isInternalServerError());
        }
    }

    // ==================== Validation Errors ====================

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("Missing required field -> 400")
        void save_MissingRequiredField_ShouldReturn400() throws Exception {
            {{EntityName}}SaveDTO dto = new {{EntityName}}SaveDTO();
            // deliberately leave required fields null

            mockMvc.perform(post("/api/{{entityName}}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Invalid field value -> 400")
        void save_InvalidValue_ShouldReturn400() throws Exception {
            {{EntityName}}SaveDTO dto = buildValidSaveDTO();
            dto.setName(""); // empty name

            mockMvc.perform(post("/api/{{entityName}}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== Pagination Edge Cases ====================

    @Nested
    @DisplayName("Pagination edge cases")
    class PaginationEdges {

        @Test
        @DisplayName("page=0 -> handled gracefully")
        void page_Zero_ShouldHandleGracefully() throws Exception {
            mockMvc.perform(get("/api/{{entityName}}/page")
                    .param("page", "0")
                    .param("pageSize", "10"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("page=-1 -> 400 or default to page 1")
        void page_Negative_ShouldReturn400() throws Exception {
            mockMvc.perform(get("/api/{{entityName}}/page")
                    .param("page", "-1")
                    .param("pageSize", "10"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("pageSize=10000 -> handled gracefully")
        void pageSize_Oversize_ShouldHandleGracefully() throws Exception {
            mockMvc.perform(get("/api/{{entityName}}/page")
                    .param("page", "1")
                    .param("pageSize", "10000"))
                    .andExpect(status().isOk());
        }
    }

    // ==================== Helpers ====================

    private {{EntityName}}SaveDTO buildValidSaveDTO() {
        // TODO: populate all required fields with valid data
        {{EntityName}}SaveDTO dto = new {{EntityName}}SaveDTO();
        // dto.setCode("TEST-001");
        // dto.setName("Test {{EntityName}}");
        return dto;
    }

    private {{EntityName}}VO buildVO(Long id) {
        // TODO: populate with matching VO fields
        {{EntityName}}VO vo = new {{EntityName}}VO();
        vo.setId(id);
        // vo.setCode("TEST-001");
        return vo;
    }
}
```

### Service Test: `{EntityName}ServiceTest.java`

```java
package {{basePackage}}.service;

import {{basePackage}}.dto.{{EntityName}}SaveDTO;
import {{basePackage}}.dto.{{EntityName}}QueryDTO;
import {{basePackage}}.entity.{{EntityName}};
import {{basePackage}}.vo.{{EntityName}}VO;
import {{basePackage}}.vo.PageResultVO;
import {{basePackage}}.repository.{{EntityName}}Repository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@DisplayName("{{EntityName}} Service")
class {{EntityName}}ServiceTest {

    @Autowired
    private {{EntityName}}Service {{entityName}}Service;

    @Autowired
    private {{EntityName}}Repository {{entityName}}Repository;

    @BeforeEach
    void setUp() {
        {{entityName}}Repository.deleteAll();
    }

    @Nested
    @DisplayName("save()")
    class Save {

        @Test
        @DisplayName("Valid DTO -> persist and return VO with generated id")
        void save_ValidDTO_ShouldPersistAndReturnVO() {
            {{EntityName}}SaveDTO dto = buildValidSaveDTO();

            {{EntityName}}VO vo = {{entityName}}Service.save(dto);

            assertThat(vo).isNotNull();
            assertThat(vo.getId()).isNotNull();
            assertThat({{entityName}}Repository.count()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Duplicate code -> throw exception")
        void save_DuplicateCode_ShouldThrow() {
            {{EntityName}}SaveDTO dto = buildValidSaveDTO();
            {{entityName}}Service.save(dto);

            assertThatThrownBy(() -> {{entityName}}Service.save(dto))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("page()")
    class PageQuery {

        @Test
        @DisplayName("Empty DB -> empty page")
        void page_EmptyDB_ShouldReturnEmptyPage() {
            {{EntityName}}QueryDTO query = new {{EntityName}}QueryDTO();
            query.setPage(1);
            query.setPageSize(10);

            PageResultVO<{{EntityName}}VO> result = {{entityName}}Service.page(query);

            assertThat(result.getRecords()).isEmpty();
            assertThat(result.getTotal()).isEqualTo(0L);
        }

        @Test
        @DisplayName("Multiple records -> correct total and page size")
        void page_MultipleRecords_ShouldReturnCorrectPage() {
            for (int i = 0; i < 15; i++) {
                {{entityName}}Service.save(buildValidSaveDTO());
            }

            {{EntityName}}QueryDTO query = new {{EntityName}}QueryDTO();
            query.setPage(1);
            query.setPageSize(10);

            PageResultVO<{{EntityName}}VO> result = {{entityName}}Service.page(query);

            assertThat(result.getRecords()).hasSize(10);
            assertThat(result.getTotal()).isEqualTo(15L);
        }
    }

    @Nested
    @DisplayName("getById()")
    class GetById {

        @Test
        @DisplayName("Existing id -> return VO")
        void getById_Existing_ShouldReturnVO() {
            {{EntityName}}VO saved = {{entityName}}Service.save(buildValidSaveDTO());

            {{EntityName}}VO found = {{entityName}}Service.getById(saved.getId());

            assertThat(found.getId()).isEqualTo(saved.getId());
        }

        @Test
        @DisplayName("Non-existing id -> throw exception")
        void getById_NotExists_ShouldThrow() {
            assertThatThrownBy(() -> {{entityName}}Service.getById(999L))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("Existing id -> delete and verify gone")
        void delete_Existing_ShouldRemove() {
            {{EntityName}}VO saved = {{entityName}}Service.save(buildValidSaveDTO());

            {{entityName}}Service.delete(saved.getId());

            assertThat({{entityName}}Repository.count()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("Audit fields")
    class AuditFields {

        @Test
        @DisplayName("Save -> CREATE_USER, CREATE_TIME, UPDATE_USER, UPDATE_TIME auto-populated")
        void save_ShouldAutoFillAuditFields() {
            {{EntityName}}VO vo = {{entityName}}Service.save(buildValidSaveDTO());

            {{EntityName}} entity = {{entityName}}Repository.findById(vo.getId()).orElseThrow();

            assertThat(entity.getCreateUser()).isNotNull();
            assertThat(entity.getCreateTime()).isNotNull();
            assertThat(entity.getUpdateUser()).isNotNull();
            assertThat(entity.getUpdateTime()).isNotNull();
        }
    }

    private {{EntityName}}SaveDTO buildValidSaveDTO() {
        // TODO: populate all required fields with valid data
        {{EntityName}}SaveDTO dto = new {{EntityName}}SaveDTO();
        // dto.setCode("TEST-" + System.currentTimeMillis());
        // dto.setName("Test {{EntityName}}");
        return dto;
    }
}
```

### Repository Test: `{EntityName}RepositoryTest.java`

```java
package {{basePackage}}.repository;

import {{basePackage}}.entity.{{EntityName}};
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
{{#jpa}}
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
{{/jpa}}
{{#mybatisPlus}}
import org.mybatis.spring.boot.test.autoconfigure.MybatisPlusTest;
{{/mybatisPlus}}
{{#jpa}}
@DataJpaTest
{{/jpa}}
{{#mybatisPlus}}
@MybatisPlusTest
{{/mybatisPlus}}
@DisplayName("{{EntityName}} Repository")
class {{EntityName}}RepositoryTest {

    @Autowired
    private {{EntityName}}Repository {{entityName}}Repository;

    @BeforeEach
    void setUp() {
        {{entityName}}Repository.deleteAll();
    }

    @Test
    @DisplayName("Insert -> auto-generated id and audit fields")
    void insert_ShouldGenerateIdAndAuditFields() {
        {{EntityName}} entity = buildEntity();
        {{entityName}}Repository.save(entity);

        assertThat(entity.getId()).isNotNull();
        assertThat(entity.getCreateTime()).isNotNull();
        assertThat(entity.getCreateUser()).isNotNull();
        assertThat(entity.getUpdateTime()).isNotNull();
        assertThat(entity.getUpdateUser()).isNotNull();
    }

    @Test
    @DisplayName("Find by code -> return matching entity")
    void findByCode_Existing_ShouldReturnEntity() {
        {{EntityName}} entity = buildEntity();
        entity.setCode("UNIQUE-001");
        {{entityName}}Repository.save(entity);

        {{EntityName}} found = {{entityName}}Repository.findByCode("UNIQUE-001");

        assertThat(found).isNotNull();
        assertThat(found.getCode()).isEqualTo("UNIQUE-001");
    }

    @Test
    @DisplayName("Find by code -> non-existing returns null")
    void findByCode_NotExists_ShouldReturnNull() {
        {{EntityName}} found = {{entityName}}Repository.findByCode("NONEXISTENT");
        assertThat(found).isNull();
    }

    @Test
    @DisplayName("Update -> UPDATE_TIME refreshed")
    void update_ShouldRefreshUpdateTime() {
        {{EntityName}} entity = buildEntity();
        {{entityName}}Repository.save(entity);

        var originalUpdateTime = entity.getUpdateTime();

        entity.setName("Updated Name");
        {{entityName}}Repository.save(entity);

        var updated = {{entityName}}Repository.findById(entity.getId()).orElseThrow();
        assertThat(updated.getUpdateTime()).isAfter(originalUpdateTime);
    }

    @Test
    @DisplayName("Delete by id -> entity removed")
    void deleteById_ShouldRemove() {
        {{EntityName}} entity = buildEntity();
        {{entityName}}Repository.save(entity);

        {{entityName}}Repository.deleteById(entity.getId());

        assertThat({{entityName}}Repository.count()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Batch insert -> correct count")
    void saveAll_ShouldInsertAll() {
        var entities = java.util.List.of(buildEntity(), buildEntity(), buildEntity());
        {{entityName}}Repository.saveAll(entities);

        assertThat({{entityName}}Repository.count()).isEqualTo(3L);
    }

    private {{EntityName}} buildEntity() {
        // TODO: populate all required fields
        {{EntityName}} entity = new {{EntityName}}();
        // entity.setCode("TEST-" + System.currentTimeMillis());
        // entity.setName("Test {{EntityName}}");
        return entity;
    }
}
```

---

## Frontend Test Template (Vitest + Vue Test Utils)

### View Component Test: `src/__tests__/views/{EntityName}List.test.js`

```javascript
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createTestingPinia } from '@pinia/testing'
import Antd from 'ant-design-vue'
import {{EntityName}}List from '@/views/{{entity-name}}/index.vue'
import * as {{entityName}}Api from '@/api/{{entityName}}'

vi.mock('@/api/{{entityName}}')

describe('{{EntityName}}List', () => {
  let wrapper

  const mockPageData = {
    records: [
      { id: 1, code: 'T-001', name: 'Test 1' },
      { id: 2, code: 'T-002', name: 'Test 2' },
    ],
    total: 2,
  }

  beforeEach(() => {
    {{entityName}}Api.page.mockResolvedValue({ code: 200, data: mockPageData })
    {{entityName}}Api.remove.mockResolvedValue({ code: 200 })

    wrapper = mount({{EntityName}}List, {
      global: {
        plugins: [
          createTestingPinia({ createSpy: vi.fn }),
          Antd,
        ],
        stubs: {
          routerLink: true,
        },
      },
    })
  })

  // ==================== Rendering ====================

  it('renders the page title', () => {
    expect(wrapper.text()).toContain('{{EntityName}}')
  })

  it('renders the table with columns', async () => {
    await wrapper.vm.$nextTick()
    const table = wrapper.findComponent({ name: 'ATable' })
    expect(table.exists()).toBe(true)
  })

  it('renders data rows after API call', async () => {
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()
    expect(wrapper.text()).toContain('T-001')
    expect(wrapper.text()).toContain('T-002')
  })

  // ==================== API Calls ====================

  it('calls page API on mount', () => {
    expect({{entityName}}Api.page).toHaveBeenCalled()
  })

  it('calls page API on pagination change', async () => {
    await wrapper.vm.$nextTick()
    // Trigger pagination...
  })

  // ==================== Search ====================

  it('triggers search on button click', async () => {
    const searchBtn = wrapper.find('[data-testid="search-btn"]')
    if (searchBtn.exists()) {
      await searchBtn.trigger('click')
      expect({{entityName}}Api.page).toHaveBeenCalledTimes(2)
    }
  })

  // ==================== Delete ====================

  it('shows confirm modal on delete click', async () => {
    await wrapper.vm.$nextTick()
    // Find delete button and trigger...
  })

  // ==================== Empty State ====================

  it('shows empty state when no data', async () => {
    {{entityName}}Api.page.mockResolvedValue({ code: 200, data: { records: [], total: 0 } })
    wrapper = mount({{EntityName}}List, {
      global: {
        plugins: [createTestingPinia({ createSpy: vi.fn }), Antd],
      },
    })
    await wrapper.vm.$nextTick()
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('No data')
  })
})
```

### API Module Test: `src/__tests__/api/{entityName}.test.js`

```javascript
import { describe, it, expect, vi, beforeEach } from 'vitest'
import request from '@/utils/request'
import * as {{entityName}}Api from '@/api/{{entityName}}'

vi.mock('@/utils/request')

describe('{{entityName}} API', () => {

  beforeEach(() => {
    vi.clearAllMocks()
    request.mockResolvedValue({ code: 200, data: {} })
  })

  it('page() calls GET with correct params', async () => {
    const params = { page: 1, pageSize: 10 }
    await {{entityName}}Api.page(params)

    expect(request).toHaveBeenCalledWith({
      url: '/api/{{entityName}}/page',
      method: 'GET',
      params,
    })
  })

  it('getById() calls GET with id', async () => {
    await {{entityName}}Api.getById(1)

    expect(request).toHaveBeenCalledWith({
      url: '/api/{{entityName}}/1',
      method: 'GET',
    })
  })

  it('save() calls POST with body', async () => {
    const data = { code: 'T-001', name: 'Test' }
    await {{entityName}}Api.save(data)

    expect(request).toHaveBeenCalledWith({
      url: '/api/{{entityName}}',
      method: 'POST',
      data,
    })
  })

  it('update() calls PUT with body', async () => {
    const data = { id: 1, code: 'T-001', name: 'Updated' }
    await {{entityName}}Api.update(data)

    expect(request).toHaveBeenCalledWith({
      url: '/api/{{entityName}}',
      method: 'PUT',
      data,
    })
  })

  it('remove() calls DELETE with id', async () => {
    await {{entityName}}Api.remove(1)

    expect(request).toHaveBeenCalledWith({
      url: '/api/{{entityName}}/1',
      method: 'DELETE',
    })
  })

  it('handles API error response', async () => {
    request.mockRejectedValue(new Error('Network error'))

    await expect({{entityName}}Api.page({})).rejects.toThrow('Network error')
  })
})
```

---

## Test Data File: `src/test/resources/test-data.sql`

```sql
-- Minimal seed data for test scenarios
-- Each test class resets state via @BeforeEach / @Transactional

-- Reference/lookup data (if applicable)
-- INSERT INTO {{tablePrefix}}dict (id, code, name, create_user, create_time, update_user, update_time)
-- VALUES (1, 'STATUS_DRAFT', 'Draft', 'SYSTEM', NOW(), 'SYSTEM', NOW());

-- Ensure sequences start at 100 to avoid collisions
-- ALTER SEQUENCE hibernate_sequence RESTART WITH 100;
```

---

## Run Commands

```bash
# Backend: all tests
mvn test

# Backend: single test class
mvn test -Dtest={{EntityName}}ControllerTest

# Backend: single test method
mvn test -Dtest={{EntityName}}ServiceTest#save_ValidDTO_ShouldPersistAndReturnVO

# Frontend: all tests
npm run test

# Frontend: single test file
npx vitest run src/__tests__/api/{{entityName}}.test.js

# Frontend: with coverage
npx vitest run --coverage
```

---

## Mandatory Coverage Targets (per entity module)

| Layer | Min Line Coverage | Key Assertions |
|-------|-------------------|----------------|
| Controller | 90%+ | All endpoints: happy path (200), validation (400), not-found (404), pagination edges |
| Service | 85%+ | Save, page, getById, delete; duplicate handling; audit-field auto-fill |
| Repository | 80%+ | Insert, findById, findByCode, update, deleteById, saveAll |
| Frontend View | 75%+ | Table renders, data displayed, search triggers API, empty state |
| Frontend API | 90%+ | Each API function called with correct method/url/params/data; error propagation |
