package org.redlance.common.requester.boosty.obj.post;

import org.jetbrains.annotations.Nullable;
import org.redlance.common.requester.boosty.obj.BoostyValues;

/**
 * Block of rich content, the format of post bodies and level descriptions.
 * <p>
 * Level descriptions consist of {@code text} and {@code image} blocks, which this record maps; other block types
 * carry fields it does not. A component the block type does not use is {@code null}, {@code 0} or {@code false}, and
 * so is an empty text.
 *
 * @param type        Block type: {@code text}, {@code image}, etc.
 * @param content     Text block: JSON array encoded as a string, {@code [text, "unstyled", [inline style ranges]]};
 *                    {@code null} in a paragraph end.
 * @param modificator Text block: {@code BLOCK_END} for a paragraph end, {@code null} otherwise.
 * @param id          Image block: uuid of the uploaded image.
 * @param url         Image block: image URL.
 * @param width       Image block: width in pixels.
 * @param height      Image block: height in pixels.
 * @param size        Image block: file size in bytes.
 * @param rendition   Image block: image rendition; unset in observed responses.
 * @param isInvalid   Image block: whether the image is broken; unset in observed responses.
 */
public record ContentBlock(
        @Nullable String type,
        @Nullable String content,
        @Nullable String modificator,
        @Nullable String id,
        @Nullable String url,
        int width,
        int height,
        long size,
        @Nullable String rendition,
        boolean isInvalid
) {
    public ContentBlock {
        type = BoostyValues.nullIfEmpty(type);
        content = BoostyValues.nullIfEmpty(content);
        modificator = BoostyValues.nullIfEmpty(modificator);
        id = BoostyValues.nullIfEmpty(id);
        url = BoostyValues.nullIfEmpty(url);
        rendition = BoostyValues.nullIfEmpty(rendition);
    }
}
