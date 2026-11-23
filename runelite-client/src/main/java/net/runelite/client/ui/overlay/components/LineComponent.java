/*
 * Copyright (c) 2018, Tomas Slusny <slusnucky@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.ui.overlay.components;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import net.runelite.client.util.Text;

@Setter
@Builder
public class LineComponent implements LayoutableRenderableEntity
{
	private String left;
	private String right;

	@Builder.Default
	private Color leftColor = Color.WHITE;

	@Builder.Default
	private Color rightColor = Color.WHITE;

	private Font leftFont;

	private Font rightFont;

	@Builder.Default
	private Point preferredLocation = new Point();

	@Builder.Default
	private Dimension preferredSize = new Dimension(ComponentConstants.STANDARD_WIDTH, 0);

	@Builder.Default
	@Getter
	private final Rectangle bounds = new Rectangle();

	private final TextComponent textComponent = new TextComponent();

	@Override
	public Dimension render(Graphics2D graphics)
	{
		// Prevent NPEs
		final String left = MoreObjects.firstNonNull(this.left, "");
		final String right = MoreObjects.firstNonNull(this.right, "");

		final Font leftFont = MoreObjects.firstNonNull(this.leftFont, graphics.getFont());
		final FontMetrics lfm = graphics.getFontMetrics(leftFont);
		final Font rightFont;
		final FontMetrics rfm;
		final int fmHeight;
		if (!right.isEmpty() || this.rightFont != null)
		{
			rightFont = MoreObjects.firstNonNull(this.rightFont, graphics.getFont());
			rfm = rightFont == leftFont ? lfm : graphics.getFontMetrics(rightFont);
			fmHeight = Math.max(lfm.getHeight(), rfm.getHeight());
		}
		else
		{
			rightFont = leftFont;
			rfm = lfm;
			fmHeight = lfm.getHeight();
		}
		final int baseX = preferredLocation.x;
		final int baseY = preferredLocation.y + fmHeight;
		int x = baseX;
		int y = baseY;
		final int leftFullWidth = left.isEmpty() ? 0 : getLineWidth(left, lfm);
		final int rightFullWidth = right.isEmpty() ? 0 : getLineWidth(right, rfm);
		final TextComponent textComponent = this.textComponent;

		if (preferredSize.width < leftFullWidth + rightFullWidth)
		{
			int leftSmallWidth = preferredSize.width;
			int rightSmallWidth = 0;

			if (!Strings.isNullOrEmpty(right))
			{
				rightSmallWidth = (preferredSize.width / 3);
				leftSmallWidth -= rightSmallWidth;
			}

			final String[] leftSplitLines = lineBreakText(left, leftSmallWidth, lfm);
			final String[] rightSplitLines = lineBreakText(right, rightSmallWidth, rfm);

			int lineCount = Math.max(leftSplitLines.length, rightSplitLines.length);

			for (int i = 0; i < lineCount; i++)
			{
				if (i < leftSplitLines.length)
				{
					final String leftText = leftSplitLines[i];
					textComponent.setPosition(x, y);
					textComponent.setText(leftText);
					textComponent.setColor(leftColor);
					textComponent.setFont(leftFont);
					textComponent.render(graphics);
				}

				if (i < rightSplitLines.length)
				{
					final String rightText = rightSplitLines[i];
					textComponent.setPosition(x + preferredSize.width - getLineWidth(rightText, rfm), y);
					textComponent.setText(rightText);
					textComponent.setColor(rightColor);
					textComponent.setFont(rightFont);
					textComponent.render(graphics);
				}

				y += fmHeight;
			}

			final Dimension dimension = new Dimension(preferredSize.width, y - baseY);
			bounds.setLocation(preferredLocation);
			bounds.setSize(dimension);
			return dimension;
		}

		if (!left.isEmpty())
		{
			textComponent.setPosition(x, y);
			textComponent.setText(left);
			textComponent.setColor(leftColor);
			textComponent.setFont(leftFont);
			textComponent.render(graphics);
		}

		if (!right.isEmpty())
		{
			textComponent.setPosition(x + preferredSize.width - rightFullWidth, y);
			textComponent.setText(right);
			textComponent.setColor(rightColor);
			textComponent.setFont(rightFont);
			textComponent.render(graphics);
		}

		y += fmHeight;

		final Dimension dimension = new Dimension(preferredSize.width, y - baseY);
		bounds.setLocation(preferredLocation);
		bounds.setSize(dimension);
		return dimension;
	}

	private static int getLineWidth(final String line, final FontMetrics metrics)
	{
		return metrics.stringWidth(Text.removeTags(line));
	}

	private static String[] lineBreakText(String text, int maxWidth, FontMetrics metrics)
	{
		final String[] words = splitChar(text, ' ');

		if (words.length == 0)
		{
			return new String[0];
		}

		final StringBuilder wrapped = new StringBuilder(words[0]);
		int spaceLeft = maxWidth - metrics.stringWidth(wrapped.toString());
		final int spaceWidth = metrics.stringWidth(" ");

		for (int i = 1; i < words.length; i++)
		{
			final String word = words[i];
			final int wordLen = metrics.stringWidth(word);

			if (wordLen + spaceWidth > spaceLeft)
			{
				wrapped.append('\n').append(word);
				spaceLeft = maxWidth - wordLen;
			}
			else
			{
				wrapped.append(' ').append(word);
				spaceLeft -= spaceWidth + wordLen;
			}
		}

		return splitChar(wrapped.toString(), '\n');
	}

	/** Equivalent to {@code s.split(String.valueOf(delim), 0)} without regex compilation. */
	private static String[] splitChar(String s, char delim)
	{
		int len = s.length();
		if (len == 0)
		{
			return new String[]{""};
		}
		List<String> parts = new ArrayList<>();
		int start = 0;
		for (int i = 0; i < len; i++)
		{
			if (s.charAt(i) == delim)
			{
				parts.add(s.substring(start, i));
				start = i + 1;
			}
		}
		parts.add(s.substring(start, len));
		int size = parts.size();
		while (size > 0 && parts.get(size - 1).isEmpty())
		{
			size--;
		}
		return parts.subList(0, size).toArray(new String[0]);
	}
}

