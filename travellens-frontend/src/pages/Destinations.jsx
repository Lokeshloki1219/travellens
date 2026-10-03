import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api from '../api/axios.js'
import { categoryGradient } from '../components/categoryStyle.js'

const CATEGORIES = ['All', 'Beach', 'Hill Station', 'Nature', 'Heritage', 'Adventure']

export default function Destinations() {
  const [destinations, setDestinations] = useState([])
  const [category, setCategory] = useState('All')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    setLoading(true)
    setError('')
    const params = category !== 'All' ? { category } : {}
    api
      .get('/destinations', { params })
      .then((res) => setDestinations(res.data))
      .catch(() => setError('Could not reach the backend. Is it running on port 8080?'))
      .finally(() => setLoading(false))
  }, [category])

  return (
    <div className="page">
      <section className="hero">
        <p className="hero-eyebrow">Discover &rarr; Explore experiences &rarr; Consult an advisor &rarr; Plan &rarr; Share</p>
        <h1>Plan a trip the way a well-travelled friend would tell it to you.</h1>
        <p className="hero-sub">
          Browse destinations with honest detail, hear from travelers who've already been there,
          and book a few minutes with someone who knows the place.
        </p>
      </section>

      <div className="chip-row">
        {CATEGORIES.map((c) => (
          <button
            key={c}
            className={`chip ${category === c ? 'chip-active' : ''}`}
            onClick={() => setCategory(c)}
          >
            {c}
          </button>
        ))}
      </div>

      {error && <p className="form-error">{error}</p>}
      {loading && <p className="muted">Loading destinations&hellip;</p>}

      <div className="destination-grid">
        {!loading && destinations.length === 0 && !error && (
          <p className="muted">No destinations in this category yet.</p>
        )}
        {destinations.map((d) => (
          <Link to={`/destinations/${d.id}`} key={d.id} className="destination-card">
            <div className="destination-banner" style={{ background: categoryGradient(d.category) }}>
              <span className="destination-category">{d.category}</span>
            </div>
            <div className="destination-body">
              <h3>{d.name}</h3>
              <p className="muted">{d.description}</p>
              <div className="destination-meta">
                <span>{d.recommendedDuration}</span>
                <span>{d.budgetEstimate}</span>
              </div>
            </div>
          </Link>
        ))}
      </div>
    </div>
  )
}
