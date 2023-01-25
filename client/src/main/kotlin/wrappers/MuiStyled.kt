@file:JsModule("@mui/material/styles")
@file:JsNonModule

package com.perpheads.files.wrappers

import csstype.Properties
import emotion.styled.StyledComponent
import emotion.styled.StyledOptions
import react.ElementType
import react.Props


external fun <P : Props> styled(
    type: ElementType<P>,
    options: StyledOptions? = definedExternally,
): ((P) -> Properties) -> StyledComponent<P>
