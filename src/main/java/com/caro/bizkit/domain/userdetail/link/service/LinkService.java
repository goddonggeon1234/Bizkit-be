package com.caro.bizkit.domain.userdetail.link.service;

import com.caro.bizkit.common.security.CardCollectionValidator;
import com.caro.bizkit.domain.ai.event.HexAnalysisTriggerEvent;
import com.caro.bizkit.domain.user.dto.UserPrincipal;
import com.caro.bizkit.domain.user.entity.User;
import com.caro.bizkit.domain.user.repository.UserRepository;
import com.caro.bizkit.domain.userdetail.link.dto.LinkRequest;
import com.caro.bizkit.domain.userdetail.link.dto.LinkResponse;
import java.util.Map;
import java.util.function.Consumer;
import com.caro.bizkit.domain.userdetail.link.entity.Link;
import com.caro.bizkit.domain.userdetail.link.repository.LinkRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class LinkService {

    private final LinkRepository linkRepository;
    private final UserRepository userRepository;
    private final CardCollectionValidator cardCollectionValidator;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<LinkResponse> getMyLinks(UserPrincipal principal) {
        return linkRepository.findAllByUserId(principal.id()).stream()
                .map(LinkResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LinkResponse> getLinksByUserId(UserPrincipal principal, Integer userId) {
        cardCollectionValidator.validateAccess(principal.id(), userId);
        return linkRepository.findAllByUserId(userId).stream()
                .map(LinkResponse::from)
                .toList();
    }

    @Transactional
    public LinkResponse createMyLink(UserPrincipal principal, LinkRequest request) {
        User user = userRepository.getReferenceById(principal.id());
        Link linkEntity = Link.create(user, request.title(), request.link());
        Link saved = linkRepository.save(linkEntity);
        if (saved.getLink().contains("github.com")) {
            eventPublisher.publishEvent(new HexAnalysisTriggerEvent(principal.id()));
        }
        return LinkResponse.from(saved);
    }

    @Transactional
    @PreAuthorize("@linkSecurity.isOwner(#linkId, authentication)")
    public LinkResponse updateMyLink(
            UserPrincipal principal,
            Integer linkId,
            Map<String, Object> request
    ) {
        Link linkEntity = linkRepository.findById(linkId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Link not found"));

        if (request == null) {
            return LinkResponse.from(linkEntity);
        }

        applyUpdates(linkEntity, request);
        return LinkResponse.from(linkEntity);
    }

    private void applyUpdates(Link linkEntity, Map<String, Object> request) {
        applyIfPresent(request, "title", linkEntity::updateTitle);
        applyIfPresent(request, "link", linkEntity::updateLink);
    }

    private void applyIfPresent(Map<String, Object> request, String key, Consumer<String> updater) {
        if (request.containsKey(key)) {
            updater.accept((String) request.get(key));
        }
    }

    @Transactional
    @PreAuthorize("@linkSecurity.isOwner(#linkId, authentication)")
    public void deleteMyLink(UserPrincipal principal, Integer linkId) {
        Link linkEntity = linkRepository.findById(linkId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Link not found"));
        linkRepository.delete(linkEntity);
    }
}
