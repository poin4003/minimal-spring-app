package com.app.core.menu;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class MenuItem {

    private String id;
    private String label;
    private String path;
    private boolean disabled;
    private boolean active;
    private boolean open;
    private List<MenuItem> children = new ArrayList<>();
}
