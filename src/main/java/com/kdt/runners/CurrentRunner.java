package com.kdt.runners;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope       // 요청마다 새 상자 (손님끼리 안 섞이게)
@Getter
@Setter
public class CurrentRunner {
    private Runner runner;      // ApiCaller는 Member member였음
}