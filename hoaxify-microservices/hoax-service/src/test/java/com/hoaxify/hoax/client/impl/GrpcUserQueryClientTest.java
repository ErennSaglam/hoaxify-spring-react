package com.hoaxify.hoax.client.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.grpc.client.GrpcChannelFactory;

import com.hoaxify.common.web.exception.BaseException;
import com.hoaxify.common.web.exception.CommonErrorType;
import com.hoaxify.grpc.user.v1.GetUserRequest;
import com.hoaxify.grpc.user.v1.GetUsersRequest;
import com.hoaxify.grpc.user.v1.GetUsersResponse;
import com.hoaxify.grpc.user.v1.UserQueryServiceGrpc;
import com.hoaxify.grpc.user.v1.UserSummary;
import com.hoaxify.hoax.dto.DtoUser;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;

/**
 * gRPC İSTEMCİSİNİ test etmek için karşıya sahte bir user-service (FakeUserService) koyuyoruz.
 * GrpcChannelFactory mock'u, istemciye bu sahte sunucuya bağlı in-process bir kanal verir.
 *
 * Tespit edilen senaryolar:
 *  findUser  : bulundu (optional alanlar null'a çevrilir) | NOT_FOUND -> Optional.empty | UNAVAILABLE -> 503
 *  findUsers : bulundu (id'ye göre map) | boş liste (çağrı yapılmaz) | sunucu hatası -> 503
 */
@ExtendWith(MockitoExtension.class)
class GrpcUserQueryClientTest {

	@Mock
	private GrpcChannelFactory channelFactory;

	private Server server;
	private ManagedChannel channel;
	private FakeUserService fakeUserService;
	private GrpcUserQueryClient client;

	/** Davranışını testin belirlediği sahte gRPC sunucusu */
	static class FakeUserService extends UserQueryServiceGrpc.UserQueryServiceImplBase {
		Status failWith;
		int getUsersCalls;

		@Override
		public void getUser(GetUserRequest request, StreamObserver<UserSummary> observer) {
			if (failWith != null) {
				observer.onError(failWith.asRuntimeException());
				return;
			}
			observer.onNext(UserSummary.newBuilder().setId(request.getId()).setUsername("user" + request.getId())
					.setEmail("u@mail.com").setImage("img.png").build());
			observer.onCompleted();
		}

		@Override
		public void getUsers(GetUsersRequest request, StreamObserver<GetUsersResponse> observer) {
			getUsersCalls++;
			if (failWith != null) {
				observer.onError(failWith.asRuntimeException());
				return;
			}
			GetUsersResponse.Builder response = GetUsersResponse.newBuilder();
			request.getIdsList().forEach(id -> response.addUsers(
					UserSummary.newBuilder().setId(id).setUsername("user" + id).setEmail("u@mail.com").build()));
			observer.onNext(response.build());
			observer.onCompleted();
		}
	}

	@BeforeEach
	void setUp() throws Exception {
		String name = InProcessServerBuilder.generateName();
		fakeUserService = new FakeUserService();
		server = InProcessServerBuilder.forName(name).directExecutor().addService(fakeUserService).build().start();
		channel = InProcessChannelBuilder.forName(name).directExecutor().build();
		when(channelFactory.createChannel("user-service")).thenReturn(channel);
		client = new GrpcUserQueryClient(channelFactory);
	}

	@AfterEach
	void tearDown() {
		channel.shutdownNow();
		server.shutdownNow();
	}

	@Test
	void findUser_success() {
		// Act
		Optional<DtoUser> result = client.findUser(3L);

		// Assert
		assertTrue(result.isPresent());
		assertEquals("user3", result.get().username());
		assertEquals("img.png", result.get().image());
		assertNull(result.get().bio(), "set edilmemiş optional alan null'a çevrilmeli");
	}

	@Test
	void findUser_notFound_shouldReturnEmpty() {
		// Arrange
		fakeUserService.failWith = Status.NOT_FOUND;

		// Act & Assert
		assertFalse(client.findUser(3L).isPresent());
	}

	@Test
	void findUser_unavailable_shouldThrowDependencyUnavailable() {
		// Arrange
		fakeUserService.failWith = Status.UNAVAILABLE;

		// Act
		BaseException exception = assertThrows(BaseException.class, () -> client.findUser(3L));

		// Assert
		assertEquals(CommonErrorType.DEPENDENCY_UNAVAILABLE, exception.getErrorType());
	}

	@Test
	void findUsers_success_shouldMapById() {
		// Act
		Map<Long, DtoUser> result = client.findUsers(List.of(1L, 2L));

		// Assert
		assertEquals(2, result.size());
		assertEquals("user2", result.get(2L).username());
	}

	@Test
	void findUsers_emptyIds_shouldNotCallServer() {
		// Act
		Map<Long, DtoUser> result = client.findUsers(List.of());

		// Assert
		assertTrue(result.isEmpty());
		assertEquals(0, fakeUserService.getUsersCalls);
	}

	@Test
	void findUsers_serverError_shouldThrowDependencyUnavailable() {
		// Arrange
		fakeUserService.failWith = Status.INTERNAL;

		// Act & Assert
		assertThrows(BaseException.class, () -> client.findUsers(List.of(1L)));
	}
}
