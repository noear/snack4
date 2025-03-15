package org.noear.snack;

/**
 * JSON 特性枚举（按读写方向分类）
 */
public enum Feature {
    //-----------------------------
    // 输入特性（反序列化）
    //-----------------------------
    /** 遇到未知属性时是否抛出异常 */
    Input_FailOnUnknownProperties(false),

    /** 是否允许使用注释 */
    Input_AllowComment(false),

    /** 自动转换字段命名风格（默认不转换） */
    Input_ConvertUnderlineStyle(false),

    //-----------------------------
    // 输出特性（序列化）
    //-----------------------------
    /** 序列化时是否输出 null 值 */
    Output_SkipNullValue(false),

    /** 输出时使用漂亮格式（带缩进） */
    Output_PrettyFormat(false),

    /** 输出字段使用下划线风格 */
    Output_UseUnderlineStyle(false),

    /** 输出枚举使用名称（默认使用名称） */
    Output_EnumUsingName(true),

    //-----------------------------
    // 通用特性（同时影响读写）
    //-----------------------------
    /** 处理大数字时使用字符串模式（避免精度丢失） */
    UseBigNumberMode(false),

    /** 转义非 ASCII 字符 */
    EscapeNonAscii(false),

    /** 使用日期格式化（默认使用时间戳） */
    UseDateFormat(false);

    private final boolean _default;
    Feature(boolean def) {
        _default = def;
    }

    public boolean enabledByDefault() { return _default; }
    public int mask() { return (1 << ordinal()); }
}