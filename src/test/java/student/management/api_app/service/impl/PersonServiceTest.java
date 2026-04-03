package student.management.api_app.service.impl;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import student.management.api_app.dto.page.PageResponse;
import student.management.api_app.dto.person.PersonCreateRequest;
import student.management.api_app.dto.person.PersonDetailResponse;
import student.management.api_app.dto.person.PersonListItemResponse;
import student.management.api_app.mapper.PersonMapper;
import student.management.api_app.model.Person;
import student.management.api_app.repository.PersonRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonServiceTest {

    @Mock
    private PersonRepository personRepository;

    @Mock
    private PersonMapper personMapper;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private PersonService personService;

    private Person person;
    private PersonDetailResponse personDetailResponse;
    private PersonListItemResponse personListItemResponse;
    private final UUID personId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        person = Person.builder()
                .id(personId)
                .fullName("Nguyen Van A")
                .dob(LocalDate.of(2000, 1, 1))
                .phone("0901234567")
                .contactEmail("a@example.com")
                .address("Ha Noi")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        personDetailResponse = new PersonDetailResponse(
                personId, "Nguyen Van A", LocalDate.of(2000, 1, 1),
                "0901234567", "a@example.com", "Ha Noi", true, Instant.now(), Instant.now());

        personListItemResponse = new PersonListItemResponse(
                personId, "Nguyen Van A", "a@example.com", true);
    }

    @Nested
    @DisplayName("getById() Tests")
    class GetByIdTests {

        @Test
        @DisplayName("GIVEN an existing ID WHEN getById THEN return PersonDetailResponse")
        void shouldReturnPersonDetailResponse_whenIdExists() {
            // Arrange
            given(personRepository.findById(personId)).willReturn(Optional.of(person));
            given(personMapper.toDetailResponse(person)).willReturn(personDetailResponse);

            // Act
            PersonDetailResponse result = personService.getById(personId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(personId);
            assertThat(result.fullName()).isEqualTo("Nguyen Van A");

            verify(personRepository).findById(personId);
            verify(personMapper).toDetailResponse(person);
        }

        @Test
        @DisplayName("GIVEN a non-existing ID WHEN getById THEN throw ResponseStatusException(404)")
        void shouldThrowException_whenIdDoesNotExist() {
            // Arrange
            UUID unknownId = UUID.randomUUID();
            given(personRepository.findById(unknownId)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> personService.getById(unknownId))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Person not found with id: " + unknownId)
                    .satisfies(e -> {
                        ResponseStatusException rse = (ResponseStatusException) e;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    });

            verify(personRepository).findById(unknownId);
            verify(personMapper, never()).toDetailResponse(any());
        }
    }

    @Nested
    @DisplayName("getByPhone() Tests")
    class GetByPhoneTests {

        @Test
        @DisplayName("GIVEN an existing phone WHEN getByPhone THEN return PersonDetailResponse")
        void shouldReturnPersonDetail_whenPhoneExists() {
            // Arrange
            String rawPhone = " 090 123 4567 ";
            String normalizedPhone = "0901234567";

            given(personRepository.findByPhone(normalizedPhone)).willReturn(Optional.of(person));
            given(personMapper.toDetailResponse(person)).willReturn(personDetailResponse);

            // Act
            PersonDetailResponse result = personService.getByPhone(rawPhone);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.phone()).isEqualTo(normalizedPhone);

            verify(personRepository).findByPhone(normalizedPhone);
            verify(personMapper).toDetailResponse(person);
        }

        @Test
        @DisplayName("GIVEN a null/empty phone WHEN getByPhone THEN throw ResponseStatusException(400)")
        void shouldThrowException_whenPhoneIsNull() {
            // Act & Assert
            assertThatThrownBy(() -> personService.getByPhone(null))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(e -> {
                        ResponseStatusException rse = (ResponseStatusException) e;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(rse.getReason()).isEqualTo("Phone is required");
                    });

            verify(personRepository, never()).findByPhone(anyString());
        }

        @Test
        @DisplayName("GIVEN a non-existing phone WHEN getByPhone THEN throw ResponseStatusException(404)")
        void shouldThrowException_whenPhoneDoesNotExist() {
            // Arrange
            String phone = "0999999999";
            given(personRepository.findByPhone(phone)).willReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> personService.getByPhone(phone))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(e -> {
                        ResponseStatusException rse = (ResponseStatusException) e;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                        assertThat(rse.getReason()).isEqualTo("Person not found with phone: " + phone);
                    });
        }
    }

    @Nested
    @DisplayName("create() Tests")
    class CreateTests {

        @Test
        @DisplayName("GIVEN valid request WHEN create THEN return created PersonDetailResponse")
        void shouldCreatePerson_whenValidRequest() {
            // Arrange
            PersonCreateRequest request = new PersonCreateRequest(
                    " Nguyen Van A ", LocalDate.of(2000, 1, 1), "+84901234567", " A@EXAMPLE.COM ", " Ha Noi \n");

            String normalizedPhone = "+84901234567";
            given(personRepository.existsByPhone(normalizedPhone)).willReturn(false);
            given(personRepository.saveAndFlush(any(Person.class))).willAnswer(invocation -> {
                Person p = invocation.getArgument(0);
                p.setId(personId); // Simulate DB ID generation
                return p;
            });
            given(personMapper.toDetailResponse(any(Person.class))).willReturn(personDetailResponse);
            // Ignore entityManager.refresh since it returns void and we don't strictly need
            // to mock its behavior unless we want to verify it's called

            // Act
            PersonDetailResponse result = personService.create(request);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(personId);

            // Verify normalizations happened inside the service logic before saving
            verify(personRepository).existsByPhone(normalizedPhone);
            verify(personRepository).saveAndFlush(any(Person.class));
            verify(entityManager).refresh(any(Person.class));
            verify(personMapper).toDetailResponse(any(Person.class));
        }

        @Test
        @DisplayName("GIVEN duplicate phone WHEN create THEN throw ResponseStatusException(409)")
        void shouldThrowException_whenPhoneAlreadyExists() {
            // Arrange
            PersonCreateRequest request = new PersonCreateRequest(
                    "Nguyen Van A", null, "0901234567", null, null);
            given(personRepository.existsByPhone("0901234567")).willReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> personService.create(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(e -> {
                        ResponseStatusException rse = (ResponseStatusException) e;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                        assertThat(rse.getReason()).contains("is existed");
                    });

            verify(personRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("GIVEN unique constraint violation during save WHEN create THEN throw ResponseStatusException(409)")
        void shouldThrowException_whenDataIntegrityViolationOccurs() {
            // Arrange
            PersonCreateRequest request = new PersonCreateRequest(
                    "Nguyen Van A", null, "0901234567", null, null);
            given(personRepository.existsByPhone("0901234567")).willReturn(false);
            given(personRepository.saveAndFlush(any(Person.class)))
                    .willThrow(new DataIntegrityViolationException("Unique constraint violation"));

            // Act & Assert
            assertThatThrownBy(() -> personService.create(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(e -> {
                        ResponseStatusException rse = (ResponseStatusException) e;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                        assertThat(rse.getReason()).isEqualTo("Unique constraint violated in DB");
                    });
        }
    }

    @Nested
    @DisplayName("getAll() Tests")
    class GetAllTests {

        @Test
        @DisplayName("GIVEN pagination request WHEN getAll THEN return PageResponse of PersonListItemResponse")
        void shouldReturnPagedPersons() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Person> personPage = new PageImpl<>(List.of(person), pageable, 1);

            given(personRepository.findAll(pageable)).willReturn(personPage);
            given(personMapper.toListItemResponse(person)).willReturn(personListItemResponse);

            // Act
            PageResponse<PersonListItemResponse> result = personService.getAll(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getItems()).hasSize(1);
            assertThat(result.getItems().get(0).id()).isEqualTo(personId);
            assertThat(result.getTotalItems()).isEqualTo(1);

            verify(personRepository).findAll(pageable);
            verify(personMapper).toListItemResponse(person);
        }
    }
}
