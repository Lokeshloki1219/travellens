const CATEGORY_GRADIENTS = {
  beach: 'linear-gradient(135deg, #1F5F5B, #5FAFA8)',
  'hill station': 'linear-gradient(135deg, #3E4C6D, #8AA399)',
  nature: 'linear-gradient(135deg, #2F5233, #7A9D54)',
  heritage: 'linear-gradient(135deg, #8B5E34, #C99A3E)',
  adventure: 'linear-gradient(135deg, #7A2E2E, #C4622D)'
}

export function categoryGradient(category) {
  if (!category) return 'linear-gradient(135deg, #1F5F5B, #C99A3E)'
  return CATEGORY_GRADIENTS[category.toLowerCase()] || 'linear-gradient(135deg, #1F5F5B, #C99A3E)'
}

export function initials(name) {
  if (!name) return '?'
  return name
    .split(' ')
    .map((w) => w[0])
    .slice(0, 2)
    .join('')
    .toUpperCase()
}
