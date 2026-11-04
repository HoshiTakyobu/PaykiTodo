using Markdig;
using Markdig.Syntax;
using Markdig.Syntax.Inlines;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Documents;
using static PaykiTodo.Desktop.NativeUi;
using Inline = Markdig.Syntax.Inlines.Inline;

namespace PaykiTodo.Desktop;

// Lightweight list preview: no WebView per card, no external requests or executable HTML.
internal static class NativeMarkdownPreview
{
    private static readonly MarkdownPipeline Pipeline = new MarkdownPipelineBuilder().UseAdvancedExtensions().Build();
    internal static FrameworkElement Create(string markdown) {
        var panel = Column();
        var document = Markdown.Parse(markdown.Length > 12000 ? markdown[..12000] : markdown, Pipeline);
        void Add(ContainerBlock blocks, string prefix = "") {
            foreach (var block in blocks) {
                if (panel.Children.Count >= 4) return;
                if (block is ListBlock list) {
                    var number = int.TryParse(list.OrderedStart, out var start) ? start : 1;
                    foreach (var child in list.OfType<ListItemBlock>()) Add(child, list.IsOrdered ? $"{number++}. " : "• ");
                } else if (block is LeafBlock leaf && block is not HtmlBlock) {
                    var text = Text("", block is HeadingBlock ? 15 : 14, block is HeadingBlock ? Ink : Muted, block is HeadingBlock);
                    text.Margin = new Thickness(0, 3, 0, 3); text.TextTrimming = TextTrimming.CharacterEllipsis;
                    text.MaxHeight = LineHeight(14) * 2;
                    if (prefix.Length > 0) text.Inlines.Add(new Run(prefix));
                    if (leaf.Inline != null) Append(text.Inlines, leaf.Inline);
                    else if (leaf is CodeBlock) { text.Inlines.Add(new Run(leaf.Lines.ToString())); text.Background = B("#F5F7FA"); }
                    if (text.Inlines.Count > 0) panel.Children.Add(text);
                } else if (block is ContainerBlock container) Add(container, prefix);
            }
        }
        Add(document);
        if (panel.Children.Count == 0) panel.Children.Add(Text("打开阅读报告查看完整内容", 14, Muted));
        return new Border { Child = panel, MaxHeight = TextSize(122), ClipToBounds = true };
    }
    private static void Append(InlineCollection target, ContainerInline parent) {
        for (Inline? node = parent.FirstChild; node != null; node = node.NextSibling) {
            switch (node) {
                case LiteralInline literal: target.Add(new Run(literal.Content.ToString())); break;
                case CodeInline code: target.Add(new Run(code.Content) { Background = B("#F5F7FA") }); break;
                case LineBreakInline: target.Add(new Run(" ")); break;
                case EmphasisInline emphasis:
                    var span = new Span(); if (emphasis.DelimiterCount >= 2) span.FontWeight = EmphasisWeight;
                    Append(span.Inlines, emphasis); target.Add(span); break;
                case HtmlEntityInline entity: target.Add(new Run(entity.Transcoded.ToString())); break;
                case ContainerInline container: Append(target, container); break;
            }
        }
    }
}
