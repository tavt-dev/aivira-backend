package com.tien.aivirabackend.exception.errorCode;

import org.springframework.http.HttpStatus;

import com.tien.aivirabackend.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AiAdviceErrorCode implements ErrorCode {
    SESSION_NOT_FOUND("E9000", "Không tìm thấy phiên tư vấn AI.", HttpStatus.NOT_FOUND),
    MESSAGE_NOT_FOUND("E9001", "Không tìm thấy tin nhắn tư vấn AI.", HttpStatus.NOT_FOUND),
    RECOMMENDATION_NOT_FOUND("E9002", "Không tìm thấy đề xuất sách.", HttpStatus.NOT_FOUND),
    MONTHLY_LIMIT_REACHED("E9003", "Bạn đã sử dụng hết 30 lượt tư vấn AI trong tháng này.",
            HttpStatus.TOO_MANY_REQUESTS),
    REQUEST_IN_PROGRESS("E9004", "Yêu cầu này đang được xử lý.", HttpStatus.CONFLICT),
    AI_ADVISOR_UNAVAILABLE("E9005", "Tư vấn AI hiện tạm thời không khả dụng. Vui lòng thử lại.",
            HttpStatus.SERVICE_UNAVAILABLE),
    INVALID_AI_RESPONSE("E9006", "AI trả về dữ liệu không hợp lệ.", HttpStatus.BAD_GATEWAY),
    INVALID_EVENT("E9007", "Dữ liệu tương tác tư vấn không hợp lệ.", HttpStatus.BAD_REQUEST),
    RAG_JOB_NOT_FOUND("E9008", "Không tìm thấy tác vụ chỉ mục RAG.", HttpStatus.NOT_FOUND),
    RAG_JOB_CONFLICT("E9009", "Một tác vụ lập lại chỉ mục RAG đang chạy.", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
