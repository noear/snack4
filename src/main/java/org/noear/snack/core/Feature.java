package org.noear.snack.core;

/**
 * JSON 特性枚举（按读写方向分类）
 */
public enum Feature {
    //-----------------------------
    // 输入特性（反序列化）
    //-----------------------------
    /**
     * 遇到未知属性时是否抛出异常
     */
    Input_FailOnUnknownProperties(false),

    /**
     * 是否允许使用注释
     */
    Input_AllowComment(false),
    /**
     * 允许单引号字符串
     */
    Input_AllowSingleQuotes(false),
    /**
     * 允许未用引号包裹的字段名
     */
    Input_AllowUnquotedFieldNames(false),
    /**
     * 允许非字符串键 (自动转换)
     */
    Input_AllowNonStringKeys(false),
    /**
     * 允许JavaScript风格的十六进制数字 (如 0x1F)
     */
    Input_AllowHexNumbers(false),
    /**
     * 允许特殊浮点值 (Infinity, -Infinity, NaN)
     */
    Input_AllowSpecialFloats(false),
    /**
     * 允许属性值省略引号 (自动推导类型)
     */
    Input_AllowAutoType(false),
    /**
     * 自动转换字段命名风格（默认不转换）
     */
    Input_ConvertUnderlineStyle(false),


    //================ 数据类型增强 ================//
    /**
     * 将数字解析为 BigDecimal (避免精度丢失)
     */
    Parse_UseBigDecimalForNumbers(false),
    /**
     * 自动解析日期格式 (支持多种格式)
     */
    Parse_AutoParseDate(true),
    /**
     * 将空字符串转为 null
     */
    Parse_EmptyStringAsNull(false),
    /**
     * 自动展开行内JSON字符串 (如 {"data": "{\"id\":1}"} )
     */
    Parse_UnwrapJsonString(false),


    //-----------------------------
    // 输出特性（序列化）
    //-----------------------------
    /**
     * 序列化时是否输出 null 值
     */
    Output_SkipNullValue(false),

    /**
     * 输出时使用漂亮格式（带缩进）
     */
    Output_PrettyFormat(false),

    /**
     * 序列化时使用单引号
     */
    Output_UseSingleQuotes(false),

    /**
     * 输出字段使用下划线风格
     */
    Output_UseUnderlineStyle(false),

    /**
     * 输出枚举使用名称（默认使用名称）
     */
    Output_EnumUsingName(true),

    /**
     * 将 Date 类型转为时间戳
     */
    Output_DateAsTimestamp(true),

    //-----------------------------
    // 通用特性（同时影响读写）
    //-----------------------------
    /**
     * 处理大数字时使用字符串模式（避免精度丢失）
     */
    UseBigNumberMode(false),

    /**
     * 转义非 ASCII 字符
     */
    EscapeNonAscii(false),

    /**
     * 使用日期格式化（默认使用时间戳）
     */
    UseDateFormat(false);

    private final boolean _default;

    Feature(boolean def) {
        _default = def;
    }

    public boolean enabledByDefault() {
        return _default;
    }

    public int mask() {
        return (1 << ordinal());
    }
}