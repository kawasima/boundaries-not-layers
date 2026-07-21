package com.example.cart.web;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 注文者フォーム。個人・法人で型を分けず、区分（{@code type}）を持つフラットな1型に、両方の項目を並べる。
 *
 * <p>個人か法人かで必須項目が変わるが、それは Bean Validation のフィールド制約だけでは書けない。だから
 * {@code @AssertTrue} のクロスフィールド検証を人手で足していく——「個人なら name 必須」「法人なら
 * companyName 必須」「法人なら corporateNumber は13桁」。sealed 型なら型の形が保証してくれるものを、
 * 条件分岐の検証で頑張って再現している。項目が増えるほどこの手当ても増える。
 */
public record OrdererForm(
        @NotBlank @Pattern(regexp = "individual|corporation", message = "type は individual か corporation")
        String type,
        @NotBlank @Email String email,
        String name,
        String companyName,
        String corporateNumber) {

    @AssertTrue(message = "個人の場合は name が必須です")
    public boolean isNameValidForIndividual() {
        return !"individual".equals(type) || (name != null && !name.isBlank());
    }

    @AssertTrue(message = "法人の場合は companyName が必須です")
    public boolean isCompanyNameValidForCorporation() {
        return !"corporation".equals(type) || (companyName != null && !companyName.isBlank());
    }

    @AssertTrue(message = "法人の場合は corporateNumber（13桁）が必須です")
    public boolean isCorporateNumberValidForCorporation() {
        return !"corporation".equals(type) || (corporateNumber != null && corporateNumber.matches("\\d{13}"));
    }
}
