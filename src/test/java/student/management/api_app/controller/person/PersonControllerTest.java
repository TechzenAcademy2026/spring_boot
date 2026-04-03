package student.management.api_app.controller.person;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import student.management.api_app.dto.page.PageResponse;
import student.management.api_app.dto.person.PersonCreateRequest;
import student.management.api_app.dto.person.PersonDetailResponse;
import student.management.api_app.dto.person.PersonListItemResponse;
import student.management.api_app.service.IPersonService;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PersonController.class)
class PersonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IPersonService personService;

    private final UUID personId = UUID.randomUUID();
    private PersonDetailResponse detailResponse;
    private PersonListItemResponse listItemResponse;

    @BeforeEach
    void setUp() {
        detailResponse = new PersonDetailResponse(
                personId,
                "Nguyen Van A",
                LocalDate.of(2000, 1, 1),
                "0901234567",
                "a@example.com",
                "Ha Noi",
                true,
                Instant.now(),
                Instant.now());

        listItemResponse = new PersonListItemResponse(
                personId,
                "Nguyen Van A",
                "a@example.com",
                true);
    }

    @Nested
    @DisplayName("GET /api/v1/persons")
    class GetAll {

        @Test
        @DisplayName("Should return 200 and page of persons")
        void shouldReturn200_whenGetAll() throws Exception {
            // Arrange
            PageResponse<PersonListItemResponse> pageResponse = new PageResponse<>(
                    new PageImpl<>(List.of(listItemResponse), PageRequest.of(0, 5), 1));
            given(personService.getAll(any(Pageable.class))).willReturn(pageResponse);

            // Act & Assert
            mockMvc.perform(get("/api/v1/persons")
                    .param("page", "0")
                    .param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.items[0].id", is(personId.toString())))
                    .andExpect(jsonPath("$.data.items[0].fullName", is("Nguyen Van A")));

            verify(personService).getAll(any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/persons/{id}")
    class GetById {

        @Test
        @DisplayName("Should return 200 and person details when found")
        void shouldReturn200_whenFound() throws Exception {
            // Arrange
            given(personService.getById(personId)).willReturn(detailResponse);

            // Act & Assert
            mockMvc.perform(get("/api/v1/persons/{id}", personId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.id", is(personId.toString())))
                    .andExpect(jsonPath("$.data.fullName", is("Nguyen Van A")))
                    .andExpect(jsonPath("$.data.phone", is("0901234567")));

            verify(personService).getById(personId);
        }

        @Test
        @DisplayName("Should return 404 when person not found")
        void shouldReturn404_whenNotFound() throws Exception {
            // Arrange
            UUID unknownId = UUID.randomUUID();
            given(personService.getById(unknownId))
                    .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Person not found"));

            // Act & Assert
            mockMvc.perform(get("/api/v1/persons/{id}", unknownId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.error.code", is("NOT_FOUND")));

            verify(personService).getById(unknownId);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/persons")
    class Create {

        @Test
        @DisplayName("Should return 201 Created with location header")
        void shouldReturn201_whenValidRequest() throws Exception {
            // Arrange
            PersonCreateRequest request = new PersonCreateRequest(
                    "Nguyen Van A", null, "0901234567", null, null);
            given(personService.create(any(PersonCreateRequest.class))).willReturn(detailResponse);

            // Act & Assert
            mockMvc.perform(post("/api/v1/persons")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(header().string("Location",
                            org.hamcrest.Matchers.containsString("/api/v1/persons/" + personId)))
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.id", is(personId.toString())))
                    .andExpect(jsonPath("$.data.fullName", is("Nguyen Van A")));

            verify(personService).create(any(PersonCreateRequest.class));
        }

        @Test
        @DisplayName("Should return 409 Conflict when phone exists")
        void shouldReturn409_whenPhoneDuplicate() throws Exception {
            // Arrange
            PersonCreateRequest request = new PersonCreateRequest(
                    "Nguyen Van A", null, "0901234567", null, null);
            given(personService.create(any(PersonCreateRequest.class)))
                    .willThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Phone is existed"));

            // Act & Assert
            mockMvc.perform(post("/api/v1/persons")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.error.code", is("CONFLICT")));
        }
    }
}
