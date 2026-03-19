import { type AnalyzerStore, type SearchResult } from '@/stores/analyzer'
import type { SymbolElement } from '@/models/SymbolElement'
import { KIND_MAPPING, typeToText } from '@/functions/analyzer'
import { Modifier } from '@/models/analyzer'
import { capitalize } from '@/functions/lang'
import type { Chip } from '@/models/Chip'

const TYPE_CHARACTER_LIMIT = 20
const PATH_CHARACTER_LIMIT = 32

export function resultToElement(result: SearchResult, analyzerStore: AnalyzerStore): SymbolElement {
  const kindInfo = KIND_MAPPING[result.key.kind]
  let headerText = kindInfo.text
  let iconName = kindInfo.icon
  let suffixText = ''
  const keyState = analyzerStore.findKeyState(result.symbol, result.key)
  if (kindInfo.abstract) {
    const abstract = new Set(keyState.properties['modifiers']).has(Modifier.ABSTRACT)
    if (abstract) {
      headerText = 'abstract ' + headerText
      iconName += '_abstract'
    }
  }
  if (kindInfo.parameterized) {
    suffixText += '(…)'
  }
  if (kindInfo.typed) {
    const type = keyState.properties['type']
    if (type != null && type.qualifiedName !== 'var') {
      suffixText += `: ${typeToText(type, TYPE_CHARACTER_LIMIT)}`
    }
  }
  headerText = capitalize(headerText)
  const parentCrumbs = analyzerStore
    .findParentCrumbs(result.key)
    .slice(0, -1)
    .map((k) => k.name)
  let parentText = ''
  for (let i = parentCrumbs.length - 1; i >= 0; i--) {
    const crumb = parentCrumbs[i]
    if (parentText.length + crumb.length + 1 > PATH_CHARACTER_LIMIT) {
      parentText = '…' + parentText
      break
    }
    parentText = `${crumb}${parentText.length === 0 ? '' : '.'}${parentText}`
  }
  headerText += ` in ${parentText}`
  const createdAt = new Date(result.symbol.keys[0].from)
  const deletedAt = result.symbol.deletedAt != null ? new Date(result.symbol.deletedAt) : undefined
  const chips = result.symbol.contributions.map(
    (contribution): Chip => ({
      username: analyzerStore.getAuthorGitHubUsername(contribution.author),
      percentage: contribution.percent,
    }),
  )

  // TODO: Remove mock data insertion
  if (result.key.name === 'idCounter') {
    chips.splice(
      0,
      1,
      ...[
        {
          username: analyzerStore.mockUsernamePrimary,
          percentage: 50,
        },
        {
          username: analyzerStore.mockUsernameSecondary,
          percentage: 50,
        },
      ],
    )
  }

  return {
    result: result.symbol,
    header: headerText,
    kind: {
      name: kindInfo.text,
      icon: iconName,
    },
    path: parentText,
    name: result.key.name,
    allNames: [],
    suffix: suffixText,
    highlights: result.match,
    chips,
    score: result.score,
    createdAt,
    deletedAt,
  }
}
