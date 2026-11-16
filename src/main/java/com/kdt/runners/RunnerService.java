package com.kdt.runners;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RunnerService {

    private final RunnerRepository runnerRepository;
    private final CurrentRunner currentRunner;

    // 도우미 1: 요청 보낸 사람 꺼내기 (없으면 401)
    private Runner requireCaller() {
        Runner caller = currentRunner.getRunner();
        if (caller == null) {
            throw new TokenFailedException();
        }
        return caller;
    }

    // 도우미 2: 크루장만 통과 (아니면 403)
    private Runner requireLeader() {
        Runner caller = requireCaller();
        if (caller.getRole() != RunnerRole.LEADER) {
            throw new ForbiddenException();
        }
        return caller;
    }

    // 목록: 크루장만
    @Transactional(readOnly = true)
    public List<RunnerResponse> list() {
        requireLeader();
        return runnerRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))   // id 오름차순
                .stream()
                .map(RunnerResponse::from)        // Runner → RunnerResponse로 하나씩 변환
                .toList();
    }

    // 단건: 본인 또는 크루장
    @Transactional(readOnly = true)
    public RunnerResponse get(Long id) {
        Runner caller = requireCaller();

        // 크루장도 아니고 본인도 아니면 → DB 보기 전에 403
        if (caller.getRole() != RunnerRole.LEADER && !caller.getId().equals(id)) {
            throw new ForbiddenException();
        }

        Runner target = runnerRepository.findById(id)
                .orElseThrow(NotFoundException::new);   // 없으면 404
        return RunnerResponse.from(target);
    }

    // 역할 변경: 크루장만, 본인은 안 됨
    @Transactional
    public RunnerResponse changeRole(Long id, RoleUpdateRequest request) {
        Runner caller = requireLeader();                        // 크루장 아니면 403

        Runner target = runnerRepository.findById(id)
                .orElseThrow(NotFoundException::new);           // 없으면 404

        if (target.getId().equals(caller.getId())) {
            throw new CannotChangeOwnRoleException();           // 본인이면 400
        }

        target.setRole(request.role());
        runnerRepository.save(target);
        return RunnerResponse.from(target);
    }

    // 삭제: 본인 또는 크루장. 단, 크루장 본인은 안 됨
    @Transactional
    public void delete(Long id) {
        Runner caller = requireCaller();

        // 크루장이 자기 자신을 → 400
        if (caller.getRole() == RunnerRole.LEADER && caller.getId().equals(id)) {
            throw new CannotDeleteSelfException();
        }
        // 크루장도 아닌데 남을 → 403 (DB 보기 전에)
        if (caller.getRole() != RunnerRole.LEADER && !caller.getId().equals(id)) {
            throw new ForbiddenException();
        }

        Runner target = runnerRepository.findById(id)
                .orElseThrow(NotFoundException::new);           // 없으면 404
        runnerRepository.delete(target);
    }
}