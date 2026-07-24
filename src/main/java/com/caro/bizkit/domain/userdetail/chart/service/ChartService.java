package com.caro.bizkit.domain.userdetail.chart.service;

import com.caro.bizkit.common.exception.CustomException;
import com.caro.bizkit.domain.card.repository.UserCardRepository;
import com.caro.bizkit.domain.user.dto.UserPrincipal;
import com.caro.bizkit.domain.userdetail.chart.dto.ChartItemResponse;
import com.caro.bizkit.domain.userdetail.chart.repository.ChartDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChartService {

    private final ChartDataRepository chartDataRepository;
    private final UserCardRepository userCardRepository;

    @Transactional(readOnly = true)
    public List<ChartItemResponse> getMyChart(UserPrincipal principal) {
        return chartDataRepository.findAllByUserId(principal.id()).stream()
                .map(ChartItemResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChartItemResponse> getUserChart(UserPrincipal principal, Integer userId) {
        if (!userCardRepository.existsCollectedCardByOwner(principal.id(), userId)) {
            throw new CustomException(HttpStatus.FORBIDDEN, "해당 사용자의 명함을 보유하고 있지 않습니다.");
        }
        return chartDataRepository.findAllByUserId(userId).stream()
                .map(ChartItemResponse::from)
                .toList();
    }
}
