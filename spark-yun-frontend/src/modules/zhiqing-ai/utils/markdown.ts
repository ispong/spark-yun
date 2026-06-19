export function renderMarkdown(markdown: string): string {
    return `<div class="zhiqing-ai-markdown">${renderMarkdownContent(markdown)}</div>`
}

export function renderMarkdownContent(markdown: string): string {
    const lines = markdown.replace(/\r\n/g, '\n').split('\n')
    const html: string[] = []
    let index = 0

    while (index < lines.length) {
        const line = lines[index]
        if (!line.trim()) {
            index += 1
            continue
        }

        const fenceMatch = line.match(/^```([^`]*)\s*$/)
        if (fenceMatch) {
            const codeLines: string[] = []
            index += 1
            while (index < lines.length && !lines[index].match(/^```\s*$/)) {
                codeLines.push(lines[index])
                index += 1
            }
            if (index < lines.length) {
                index += 1
            }
            const language = fenceMatch[1]?.trim() || '代码'
            const languageClass = language && language !== '代码' ? language.replace(/[^\w-]/g, '') : ''
            html.push(
                `<div class="zhiqing-ai-code-block"><div class="zhiqing-ai-code-block__header"><span>${escapeHtml(
                    language
                )}</span><button type="button" class="zhiqing-ai-code-block__copy">复制</button></div><pre><code${
                    languageClass ? ` class="language-${escapeHtml(languageClass)}"` : ''
                }>${escapeHtml(codeLines.join('\n'))}</code></pre></div>`
            )
            continue
        }

        if (isTableStart(lines, index)) {
            const headers = splitTableLine(lines[index])
            const rows: string[][] = []
            index += 2
            while (index < lines.length && lines[index].trim().startsWith('|')) {
                rows.push(splitTableLine(lines[index]))
                index += 1
            }
            html.push(renderTable(headers, rows))
            continue
        }

        const headingMatch = line.match(/^(#{1,6})\s+(.+)$/)
        if (headingMatch) {
            const level = headingMatch[1].length
            html.push(`<h${level}>${renderInlineMarkdown(headingMatch[2])}</h${level}>`)
            index += 1
            continue
        }

        if (/^>\s?/.test(line)) {
            const quoteLines: string[] = []
            while (index < lines.length && /^>\s?/.test(lines[index])) {
                quoteLines.push(lines[index].replace(/^>\s?/, ''))
                index += 1
            }
            html.push(`<blockquote>${renderMarkdownContent(quoteLines.join('\n'))}</blockquote>`)
            continue
        }

        if (/^\s*[-*+]\s+/.test(line)) {
            const items: string[] = []
            while (index < lines.length && /^\s*[-*+]\s+/.test(lines[index])) {
                items.push(lines[index].replace(/^\s*[-*+]\s+/, ''))
                index += 1
            }
            html.push(`<ul>${items.map((item) => `<li>${renderInlineMarkdown(item)}</li>`).join('')}</ul>`)
            continue
        }

        if (/^\s*\d+\.\s+/.test(line)) {
            const items: string[] = []
            while (index < lines.length && /^\s*\d+\.\s+/.test(lines[index])) {
                items.push(lines[index].replace(/^\s*\d+\.\s+/, ''))
                index += 1
            }
            html.push(`<ol>${items.map((item) => `<li>${renderInlineMarkdown(item)}</li>`).join('')}</ol>`)
            continue
        }

        const paragraphLines: string[] = []
        while (
            index < lines.length &&
            lines[index].trim() &&
            !lines[index].match(/^```([^`]*)\s*$/) &&
            !lines[index].match(/^(#{1,6})\s+/) &&
            !/^>\s?/.test(lines[index]) &&
            !/^\s*[-*+]\s+/.test(lines[index]) &&
            !/^\s*\d+\.\s+/.test(lines[index]) &&
            !isTableStart(lines, index)
        ) {
            paragraphLines.push(lines[index])
            index += 1
        }
        html.push(`<p>${renderInlineMarkdown(paragraphLines.join('\n'))}</p>`)
    }

    return html.join('')
}

export function escapeHtml(text: string): string {
    return text
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;')
}

function renderInlineMarkdown(text: string): string {
    const codeValues: string[] = []
    let html = escapeHtml(text).replace(/`([^`]+)`/g, (_match, code) => {
        codeValues.push(`<code>${code}</code>`)
        return `@@CODE_${codeValues.length - 1}@@`
    })

    html = html
        .replace(/!\[([^\]]*)\]\((https?:\/\/[^)\s]+)\)/g, '<img alt="$1" src="$2" />')
        .replace(/\[([^\]]+)\]\((https?:\/\/[^)\s]+)\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>')
        .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
        .replace(/~~([^~]+)~~/g, '<del>$1</del>')
        .replace(/(^|[^*])\*([^*]+)\*/g, '$1<em>$2</em>')
        .replace(/\n/g, '<br />')

    codeValues.forEach((code, codeIndex) => {
        html = html.replace(`@@CODE_${codeIndex}@@`, code)
    })

    return html
}

function isTableStart(lines: string[], index: number): boolean {
    return (
        index + 1 < lines.length &&
        lines[index].trim().startsWith('|') &&
        /^\s*\|?[\s:-]+\|[\s|:-]*$/.test(lines[index + 1])
    )
}

function splitTableLine(line: string): string[] {
    return line
        .trim()
        .replace(/^\|/, '')
        .replace(/\|$/, '')
        .split('|')
        .map((cell) => cell.trim())
}

function renderTable(headers: string[], rows: string[][]): string {
    return `<div class="zhiqing-ai-table-wrap"><table><thead><tr>${headers
        .map((header) => `<th>${renderInlineMarkdown(header)}</th>`)
        .join('')}</tr></thead><tbody>${rows
        .map((row) => `<tr>${headers.map((_header, cellIndex) => `<td>${renderInlineMarkdown(row[cellIndex] || '')}</td>`).join('')}</tr>`)
        .join('')}</tbody></table></div>`
}
