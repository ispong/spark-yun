package com.isxcode.star.api.view.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EchartTitle {

	private String text;

	private String left;

	private String top;

	private EchartTextStyle textStyle;

	private String subText;

}
