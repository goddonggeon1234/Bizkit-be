package com.caro.bizkit.domain.userdetail.link.security;

import com.caro.bizkit.domain.user.dto.UserPrincipal;
import com.caro.bizkit.domain.userdetail.link.entity.Link;
import com.caro.bizkit.domain.userdetail.link.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component("linkSecurity")
@RequiredArgsConstructor
public class LinkSecurity {

    private final LinkRepository linkRepository;

    public boolean isOwner(Integer linkId, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return false;
        }
        Link link = linkRepository.findById(linkId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Link not found"));
        return link.getUser().getId().equals(principal.id());
    }
}
