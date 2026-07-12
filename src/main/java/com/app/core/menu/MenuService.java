package com.app.core.menu;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class MenuService {

    private static final String MENU_RESOURCE = "ui/menu/menu.json";

    private final ObjectMapper objectMapper;

    public List<MenuItem> getMenuTree(String currentPath) {
        return readMenuTree().stream()
                .map(item -> markState(item, currentPath))
                .toList();
    }

    private List<MenuItem> readMenuTree() {
        try (InputStream inputStream = new ClassPathResource(MENU_RESOURCE).getInputStream()) {
            return objectMapper.readValue(inputStream, new TypeReference<List<MenuItem>>() {
            });
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot load menu from " + MENU_RESOURCE, ex);
        }
    }

    private MenuItem markState(MenuItem source, String currentPath) {
        MenuItem item = new MenuItem();
        item.setId(source.getId());
        item.setLabel(source.getLabel());
        item.setPath(source.getPath());
        item.setDisabled(source.isDisabled());

        List<MenuItem> children = source.getChildren() == null
                ? List.of()
                : source.getChildren().stream()
                        .map(child -> markState(child, currentPath))
                        .toList();

        boolean active = matchesPath(source.getPath(), currentPath);
        boolean open = active || children.stream().anyMatch(child -> child.isActive() || child.isOpen());

        item.setChildren(children);
        item.setActive(active);
        item.setOpen(open);
        return item;
    }

    private boolean matchesPath(String itemPath, String currentPath) {
        if (itemPath == null || currentPath == null) {
            return false;
        }

        return currentPath.equals(itemPath) || currentPath.startsWith(itemPath + "/");
    }
}
