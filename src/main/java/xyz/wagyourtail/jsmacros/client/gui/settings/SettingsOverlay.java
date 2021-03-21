package xyz.wagyourtail.jsmacros.client.gui.settings;

import com.google.common.collect.Lists;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import xyz.wagyourtail.jsmacros.client.gui.elements.Button;
import xyz.wagyourtail.jsmacros.client.gui.overlays.IOverlayParent;
import xyz.wagyourtail.jsmacros.client.gui.overlays.OverlayContainer;
import xyz.wagyourtail.jsmacros.core.Core;
import xyz.wagyourtail.jsmacros.core.config.Option;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class SettingsOverlay extends OverlayContainer {
    private final Text title = new TranslatableText("jsmacros.settings");
    private CategoryTreeContainer sections;
    private AbstractSettingGroupContainer category;
    private final SettingTree settings = new SettingTree();
    public SettingsOverlay(int x, int y, int width, int height, TextRenderer textRenderer, IOverlayParent parent) {
        super(x, y, width, height, textRenderer, parent);
    
        for (Class<?> clazz : Core.instance.config.optionClasses.values()) {
            for (Field f : clazz.getDeclaredFields()) {
                if (f.isAnnotationPresent(Option.class)) {
                    try {
                        Option option = f.getAnnotation(Option.class);
                        Method getter = null;
                        Method setter = null;
                        if (!option.getter().equals("")) {
                            getter = clazz.getDeclaredMethod(option.getter());
                        }
                        if (!option.setter().equals("")) {
                            setter = clazz.getDeclaredMethod(option.setter(), f.getType());
                        }
                        settings.addChild(option.group(), new SettingField<>(option, Core.instance.config.getOptions(clazz), f, getter, setter, f.getType()));
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    }
                }
            }
            //synthetics
            for (Method m : clazz.getDeclaredMethods()) {
                if (m.isAnnotationPresent(Option.class)) {
                    try {
                        Option option = m.getAnnotation(Option.class);
                        Method setter = null;
                        if (!option.setter().equals("")) {
                            setter = clazz.getDeclaredMethod(option.setter(), m.getReturnType());
                        }
                        settings.addChild(option.group(), new SettingField<>(option, Core.instance.config.getOptions(clazz), null, m, setter, m.getReturnType()));
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    
    @Override
    public void init() {
        super.init();
        int w = width - 4;
    
        this.addButton(new Button(x + width - 12, y + 2, 10, 10, textRenderer, 0, 0x7FFFFFFF, 0x7FFFFFFF, 0xFFFFFF, new LiteralText("X"), (btn) -> this.close()));
        sections = new CategoryTreeContainer(x + 2, y + 13, w / 3, height - 17, textRenderer, this);
        
        for (String[] group : settings.groups()) {
            sections.addCategory(group);
        }
    }
    
    public void clearCategory() {
        if (category != null) {
            category.getButtons().forEach(this::removeButton);
            category = null;
        }
    }
    
    public void selectCategory(String[] category) {
        int w = width - 4;
        clearCategory();
        List<SettingField<?>> settings = this.settings.getSettings(category);
        if (settings.size() != 1 || settings.get(0).isSimple()) {
            this.category = new SettingGroupContainer(x + 3 + w / 3, y + 13, 2 * w / 3, height - 17, textRenderer, this, category);
            for (SettingField<?> field : settings) {
                this.category.addSetting(field);
            }
        } else {
            if (settings.get(0).option.type().value().equals("color")) {
            
            }
        }
    }
    
    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        int w = width - 4;
        
        sections.render(matrices, mouseX, mouseY, delta);
        
        textRenderer.drawTrimmed(title, x + 3, y + 3, width - 14, 0xFFFFFF);
        fill(matrices, x + 2, y + 12, x + width - 2, y + 13, 0xFFFFFFFF);
        
        //sep
        fill(matrices, x + w / 3, y + 13, x + w / 3 + 1, y + height, 0xFFFFFFFF);
        
        if (category != null) {
            category.render(matrices, mouseX, mouseY, delta);
        }
        
        super.render(matrices, mouseX, mouseY, delta);
    }
    
    static class SettingTree {
        Map<String, SettingTree> children = new HashMap<>();
        List<SettingField<?>> settings = new LinkedList<>();
        
        void addChild(String[] group, SettingField<?> field) {
            if (group.length > 0) {
                String[] childGroup = new String[group.length - 1];
                System.arraycopy(group, 1, childGroup, 0, childGroup.length);
                children.computeIfAbsent(group[0], (key) -> new SettingTree()).addChild(childGroup, field);
            } else {
                settings.add(field);
            }
        }
        
        public List<String[]> groups() {
            if (children.size() > 0) {
                List<String[]> groups = new LinkedList<>();
                for (Map.Entry<String, SettingTree> child : children.entrySet()) {
                    for (String[] childGroup : child.getValue().groups()) {
                        String[] group = new String[childGroup.length + 1];
                        System.arraycopy(childGroup, 0, group, 1, childGroup.length);
                        group[0] = child.getKey();
                        groups.add(group);
                    }
                    groups.add(new String[] {child.getKey()});
                }
                return groups;
            }
            return new LinkedList<>();
        }
        
        public List<SettingField<?>> getSettings(String[] group) {
            if (group.length > 0) {
                String[] childGroup = new String[group.length - 1];
                System.arraycopy(group, 1, childGroup, 0, childGroup.length);
                return children.get(group[0]).getSettings(childGroup);
            }
            return settings;
        }
    }
    
    public static class SettingField<T> {
        public final Class<T> type;
        public final Option option;
        final Object containingClass;
        final Field field;
        final Method getter;
        final Method setter;
        
        public SettingField(Option option, Object containingClass, Field f, Method getter, Method setter, Class<T> type) {
            this.option = option;
            this.containingClass = containingClass;
            this.field = f;
            this.getter = getter;
            this.setter = setter;
            this.type = type;
        }
        
        public void set(T o) throws IllegalAccessException, InvocationTargetException {
            if (setter == null) {
                field.set(containingClass, o);
            } else {
                setter.invoke(containingClass, o);
            }
        }
        
        @SuppressWarnings("unchecked")
        public T get() throws IllegalAccessException, InvocationTargetException {
            if (getter == null) {
                return (T) field.get(containingClass);
            } else {
                return (T) getter.invoke(containingClass);
            }
        }
        
        public boolean hasOptions() {
            return !option.options().equals("") || type.isEnum();
        }
        
        @SuppressWarnings("unchecked")
        public List<T> getOptions() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
            if (!option.options().equals(""))
                return (List<T>) containingClass.getClass().getDeclaredMethod(option.options()).invoke(containingClass);
            if (type.isEnum())
                return Lists.newArrayList(type.getEnumConstants());
            return null;
        }
        
        public boolean isSimple() {
            return !List.class.isAssignableFrom(type) && !Map.class.isAssignableFrom(type) && !type.isArray();
        }
    }
}