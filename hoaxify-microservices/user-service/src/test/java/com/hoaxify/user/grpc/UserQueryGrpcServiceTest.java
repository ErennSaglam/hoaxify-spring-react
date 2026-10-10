package com.hoaxify.user.grpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hoaxify.common.web.exception.BaseException;
import com.hoaxify.grpc.user.v1.GetUserRequest;
import com.hoaxify.grpc.user.v1.GetUsersRequest;
import com.hoaxify.grpc.user.v1.GetUsersResponse;
import com.hoaxify.grpc.user.v1.UserQueryServiceGrpc;
import com.hoaxify.grpc.user.v1.UserQueryServiceGrpc.UserQueryServiceBlockingStub;
import com.hoaxify.grpc.user.v1.UserSummary;
import com.hoaxify.user.dto.DtoUser;
import com.hoaxify.user.exception.UserErrorType;
import com.hoaxify.user.mapper.UserMapper;
import com.hoaxify.user.services.IUserProfileService;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;

/**
 * gRPC sunucusunu GERÇEK bir gRPC çağrısıyla test ediyoruz, ama ağ açmadan: in-process sunucu
 * aynı JVM içinde çalışır, istemci ile arasındaki "kablo" bellektedir. Protobuf serileştirme,
 * status kodları ve stub'lar gerçek; sadece servis katmanı mock.
 *
 * Tespit edilen senaryolar:
 *  GetUser  : success | kullanıcı yok -> NOT_FOUND durumu
 *  GetUsers : birden çok kullanıcı | boş sonuç
 */
@ExtendWith(MockitoExtension.class)
class UserQueryGrpcServiceTest {

	@Mock
	private IUserProfileService userProfileService;

	private Server server;
	private ManagedChannel channel;
	private UserQueryServiceBlockingStub stub;

	@BeforeEach
	void setUp() throws Exception {
		String name = InProcessServerBuilder.generateName();
		UserQueryGrpcService service = new UserQueryGrpcService(userProfileService, new UserMapper());
		server = InProcessServerBuilder.forName(name).directExecutor().addService(service).build().start();
		channel = InProcessChannelBuilder.forName(name).directExecutor().build();
		stub = UserQueryServiceGrpc.newBlockingStub(channel);
	}

	@AfterEach
	void tearDown() {
		channel.shutdownNow();
		server.shutdownNow();
	}

	@Test
	void getUser_success() {
		// Arrange
		when(userProfileService.getUser(1L)).thenReturn(new DtoUser(1L, "user1", "u1@mail.com", "img.png", null));

		// Act
		UserSummary result = stub.getUser(GetUserRequest.newBuilder().setId(1).build());

		// Assert
		assertEquals(1L, result.getId());
		assertEquals("user1", result.getUsername());
		assertEquals("img.png", result.getImage());
		assertFalse(result.hasBio());
		verify(userProfileService, times(1)).getUser(1L);
	}

	@Test
	void getUser_notFound_shouldReturnNotFoundStatus() {
		// Arrange
		when(userProfileService.getUser(99L)).thenThrow(new BaseException(UserErrorType.USER_NOT_FOUND, "99"));

		// Act
		StatusRuntimeException exception = assertThrows(StatusRuntimeException.class,
				() -> stub.getUser(GetUserRequest.newBuilder().setId(99).build()));

		// Assert: istemci tarafında HTTP 404'ün gRPC karşılığı
		assertEquals(Status.Code.NOT_FOUND, exception.getStatus().getCode());
	}

	@Test
	void getUsers_success() {
		// Arrange
		when(userProfileService.getUsersByIds(List.of(1L, 2L))).thenReturn(List.of(
				new DtoUser(1L, "user1", "u1@mail.com", null, null),
				new DtoUser(2L, "user2", "u2@mail.com", null, null)));

		// Act
		GetUsersResponse result = stub.getUsers(GetUsersRequest.newBuilder().addIds(1).addIds(2).build());

		// Assert
		assertEquals(2, result.getUsersCount());
		assertEquals("user2", result.getUsers(1).getUsername());
	}

	@Test
	void getUsers_emptyList() {
		// Arrange
		when(userProfileService.getUsersByIds(List.of(5L))).thenReturn(List.of());

		// Act
		GetUsersResponse result = stub.getUsers(GetUsersRequest.newBuilder().addIds(5).build());

		// Assert
		assertEquals(0, result.getUsersCount());
	}
}
