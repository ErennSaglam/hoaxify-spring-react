package com.hoaxify.hoax.client.impl;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.stereotype.Component;

import com.hoaxify.common.web.exception.BaseException;
import com.hoaxify.common.web.exception.CommonErrorType;
import com.hoaxify.grpc.user.v1.GetUserRequest;
import com.hoaxify.grpc.user.v1.GetUsersRequest;
import com.hoaxify.grpc.user.v1.UserQueryServiceGrpc;
import com.hoaxify.grpc.user.v1.UserQueryServiceGrpc.UserQueryServiceBlockingStub;
import com.hoaxify.grpc.user.v1.UserSummary;
import com.hoaxify.hoax.client.IUserQueryClient;
import com.hoaxify.hoax.dto.DtoUser;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;

/**
 * gRPC İSTEMCİSİ. Stub (.proto'dan üretilen istemci), uzaktaki metodu yerel metot gibi çağırmamızı sağlar:
 *     stub.getUser(GetUserRequest.newBuilder().setId(5).build())
 *
 * Kanal (channel) config-server'daki spring.grpc.client.channels.user-service ayarından oluşturulur
 * (adres: static://localhost:9095). Kanal pahalıdır ve thread-safe'tir; bir kez oluşturulup paylaşılır.
 *
 * Deadline: her çağrıya zaman sınırı konur. Konmazsa user-service yanıt vermediğinde
 * hoax-service'in thread'leri sonsuza dek bekler ve zincirleme çöküş (cascading failure) olur.
 */
@Slf4j
@Component
public class GrpcUserQueryClient implements IUserQueryClient {

	static final long DEADLINE_MILLIS = 2000;

	private final UserQueryServiceBlockingStub stub;

	/*
	 * Tek constructor: Spring onu otomatik kullanır. Testte GrpcChannelFactory mock'lanıp
	 * gerçek bir in-process (ağsız) kanal verilir.
	 */
	public GrpcUserQueryClient(GrpcChannelFactory channelFactory) {
		this.stub = UserQueryServiceGrpc.newBlockingStub(channelFactory.createChannel("user-service"));
	}

	@Override
	public Optional<DtoUser> findUser(Long userId) {
		try {
			UserSummary user = withDeadline().getUser(GetUserRequest.newBuilder().setId(userId).build());
			return Optional.of(toDto(user));
		} catch (StatusRuntimeException ex) {
			if (ex.getStatus().getCode() == Status.Code.NOT_FOUND) {
				return Optional.empty();
			}
			throw unavailable(ex);
		}
	}

	@Override
	public Map<Long, DtoUser> findUsers(Collection<Long> userIds) {
		if (userIds.isEmpty()) {
			return Map.of();
		}
		try {
			return withDeadline().getUsers(GetUsersRequest.newBuilder().addAllIds(userIds).build())
					.getUsersList().stream()
					.map(this::toDto)
					.collect(Collectors.toMap(DtoUser::id, Function.identity()));
		} catch (StatusRuntimeException ex) {
			throw unavailable(ex);
		}
	}

	private UserQueryServiceBlockingStub withDeadline() {
		return stub.withDeadlineAfter(DEADLINE_MILLIS, TimeUnit.MILLISECONDS);
	}

	/** UNAVAILABLE (servis kapalı), DEADLINE_EXCEEDED (yavaş) vb. -> istemciye 503 */
	private BaseException unavailable(StatusRuntimeException ex) {
		log.warn("user-service gRPC call failed: {}", ex.getStatus());
		return new BaseException(CommonErrorType.DEPENDENCY_UNAVAILABLE);
	}

	/** protobuf -> DTO. optional alanlar hasXxx() ile kontrol edilir (proto3'te null yoktur) */
	private DtoUser toDto(UserSummary user) {
		return new DtoUser(
				user.getId(),
				user.getUsername(),
				user.getEmail(),
				user.hasImage() ? user.getImage() : null,
				user.hasBio() ? user.getBio() : null);
	}
}
