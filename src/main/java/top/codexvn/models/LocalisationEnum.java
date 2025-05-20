package top.codexvn.models;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LocalisationEnum {
    ENGLISH("英语","l_english","english"),
    BRAZ_POR("巴西葡萄牙语","l_braz_por","braz_por"),
    GERMAN("德语","l_german","german"),
    FRENCH("法国","l_french","french"),
    SPANISH("西班牙语","l_spanish","spanish"),
    POLISH("波兰语","l_polish","polish"),
    RUSSIAN("俄语","l_russian","russian"),
    SIMP_CHINESE("简体中文","l_simp_chinese","simp_chinese"),
    JAPANESE("日语","l_japanese","japanese"),
    KOREAN("韩语","l_korean","korean");
    private final String name;
    private final String value;
    private final String dirName;
}
