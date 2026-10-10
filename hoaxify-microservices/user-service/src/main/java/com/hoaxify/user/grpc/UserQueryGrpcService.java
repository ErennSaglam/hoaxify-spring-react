package com.hoaxify.user.grpc;

import org.springframework.stereotype.Service;

import com.hoaxify.common.web.exception.BaseException;
import com.hoaxify.grpc.user.v1.GetUserRequest;
import com.hoaxify.grpc.user.v1.GetUsersRequest;
import com.hoaxify.grpc.user.v1.GetUsersResponse;
import com.hoaxify.grpc.user.v1.UserQueryServiceGrpc;
import com.hoaxify.grpc.user.v1.UserSummary;
import com.hoaxify.user.dto.DtoUser;
import com.hoaxify.user.mapper.UserMapper;
import com.hoaxify.user.services.IUserProfileService;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;

/**
 * gRPC sunucusu. UserQueryServiceImplBase, .proto dosyasından ÜRETİLEN iskelet sınıftır;
 * biz sadece metotları doldururuz. Spring gRPC, bu bean'i bulup 9095 portunda yayına alır.
 *
 * Bu sınıf bir "adapter": REST controller'ı gibi, sadece protokol çevirisi yapar ve
 * aynı servis katmanını (IUserProfileService) çağırır. İş mantığı burada değil.
 *
 * gRPC'de dönüş değeri yok; cevap StreamObserver'a yazılır:
 *   onNext(cevap) -> onCompleted()   (başarı)
 *   onError(Status...)               (hata; HTTP status kodlarının gRPC karşılığı)
 */
@Service
@RequiredArgsConstructor
public class UserQueryGrpcService extends UserQueryServiceGrpc.UserQueryServiceImplBase {

	private final IUserProfileService userProfileService;
	private final UserMapper userMapper;

	@Override
	public void getUser(GetUserRequest request, StreamObserver<UserSummary> responseObserver) {
		DtoUser user;
		try {
			user = userProfileService.getUser(request.getId());
		} catch (BaseException ex) {
			// HTTP 404'ün gRPC karşılığı NOT_FOUND
			responseObserver.onError(Status.NOT_FOUND
					.withDescription("User " + request.getId() + " not found")
					.asRuntimeException());
			return;
		}
		responseObserver.onNext(userMapper.toProto(user));
		responseObserver.onCompleted();
	}

	@Override
	public void getUsers(GetUsersRequest request, StreamObserver<GetUsersResponse> responseObserver) {
		GetUsersResponse.Builder response = GetUsersResponse.newBuilder();
		userProfileService.getUsersByIds(request.getIdsList())
				.forEach(user -> response.addUsers(userMapper.toProto(user)));
		responseObserver.onNext(response.build());
		responseObserver.onCompleted();
	}
}
