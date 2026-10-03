import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import api from '../api/axios.js'
import { useAuth } from '../context/AuthContext.jsx'
import { categoryGradient, initials } from '../components/categoryStyle.js'

function AdvisorCard({ advisor }) {
  const { user } = useAuth()
  const [slots, setSlots] = useState(null)
  const [open, setOpen] = useState(false)
  const [message, setMessage] = useState('')
  const [booking, setBooking] = useState(false)

  const loadSlots = () => {
    setOpen((prev) => !prev)
    if (slots === null) {
      api
        .get(`/advisors/${advisor.id}/availability`, { params: { onlyOpen: true } })
        .then((res) => setSlots(res.data))
        .catch(() => setSlots([]))
    }
  }

  const reserve = async (slot) => {
    setMessage('')
    setBooking(true)
    try {
      await api.post('/bookings', { availabilityId: slot.id })
      setSlots((prev) => prev.filter((s) => s.id !== slot.id))
      setMessage(`Booked with ${advisor.name} on ${slot.date} at ${slot.startTime}.`)
    } catch (err) {
      setMessage(err.response?.data?.message || 'That slot was just taken. Try another one.')
      setSlots((prev) => prev.filter((s) => s.id !== slot.id))
    } finally {
      setBooking(false)
    }
  }

  return (
    <div className="advisor-card">
      <div className="advisor-avatar">{initials(advisor.name)}</div>
      <div className="advisor-info">
        <h4>{advisor.name}</h4>
        <p className="muted">{advisor.bio}</p>
        <p className="advisor-experience">{advisor.yearsOfExperience} years guiding travelers here</p>

        {!user && <p className="muted small">Log in to reserve a consultation slot.</p>}

        {user && (
          <>
            <button className="btn btn-outline btn-small" onClick={loadSlots}>
              {open ? 'Hide availability' : 'View availability'}
            </button>

            {open && (
              <div className="slot-list">
                {slots === null && <p className="muted small">Loading slots&hellip;</p>}
                {slots && slots.length === 0 && <p className="muted small">No open slots right now.</p>}
                {slots &&
                  slots.map((slot) => (
                    <button
                      key={slot.id}
                      className="slot-chip"
                      disabled={booking}
                      onClick={() => reserve(slot)}
                    >
                      {slot.date} &middot; {slot.startTime}
                    </button>
                  ))}
              </div>
            )}

            {message && <p className="form-note">{message}</p>}
          </>
        )}
      </div>
    </div>
  )
}

export default function DestinationDetail() {
  const { id } = useParams()
  const { user } = useAuth()
  const [destination, setDestination] = useState(null)
  const [advisors, setAdvisors] = useState([])
  const [posts, setPosts] = useState([])
  const [error, setError] = useState('')

  useEffect(() => {
    api.get(`/destinations/${id}`).then((res) => setDestination(res.data)).catch(() => setError('Destination not found.'))
    api.get('/advisors', { params: { destinationId: id } }).then((res) => setAdvisors(res.data))
    api.get('/posts', { params: { destinationId: id } }).then((res) => setPosts(res.data))
  }, [id])

  if (error) return <div className="page"><p className="form-error">{error}</p></div>
  if (!destination) return <div className="page"><p className="muted">Loading&hellip;</p></div>

  const attractions = (destination.mustVisitAttractions || '').split(',').map((s) => s.trim()).filter(Boolean)
  const avoid = (destination.thingsToAvoid || '').split('.').map((s) => s.trim()).filter(Boolean)

  return (
    <div className="page">
      <div className="detail-banner" style={{ background: categoryGradient(destination.category) }}>
        <span className="destination-category">{destination.category}</span>
        <h1>{destination.name}</h1>
      </div>

      <p className="lead">{destination.description}</p>

      <div className="fact-grid">
        <div className="fact">
          <span className="fact-label">Best time to visit</span>
          <span className="fact-value">{destination.bestTimeToVisit}</span>
        </div>
        <div className="fact">
          <span className="fact-label">Estimated budget</span>
          <span className="fact-value">{destination.budgetEstimate}</span>
        </div>
        <div className="fact">
          <span className="fact-label">Recommended duration</span>
          <span className="fact-value">{destination.recommendedDuration}</span>
        </div>
      </div>

      <div className="two-col">
        <section>
          <h3>Must-visit</h3>
          <ul className="plain-list">
            {attractions.map((a) => (
              <li key={a}>{a}</li>
            ))}
          </ul>
        </section>
        <section>
          <h3>Good to know</h3>
          <p className="muted">{destination.tips}</p>
          {avoid.length > 0 && (
            <>
              <h4>Avoid</h4>
              <ul className="plain-list">
                {avoid.map((a) => (
                  <li key={a}>{a}</li>
                ))}
              </ul>
            </>
          )}
        </section>
      </div>

      <section className="section-block">
        <h3>Talk to an advisor</h3>
        {advisors.length === 0 && <p className="muted">No advisors listed for this destination yet.</p>}
        <div className="advisor-list">
          {advisors.map((a) => (
            <AdvisorCard advisor={a} key={a.id} />
          ))}
        </div>
      </section>

      <section className="section-block">
        <div className="section-header-row">
          <h3>Traveler experiences</h3>
          {user && <Link to="/share" className="btn btn-outline btn-small">Share yours</Link>}
        </div>
        {posts.length === 0 && <p className="muted">No traveler posts yet &mdash; be the first to share.</p>}
        <div className="post-grid">
          {posts.map((p) => (
            <article className="post-card" key={p.id}>
              {p.mediaUrl && p.mediaType === 'IMAGE' && (
                <img src={p.mediaUrl} alt={p.caption || 'Traveler photo'} className="post-media" />
              )}
              {p.mediaUrl && p.mediaType === 'VIDEO' && (
                <video src={p.mediaUrl} controls className="post-media" />
              )}
              <div className="post-body">
                <p>{p.caption}</p>
                {p.tip && <p className="muted small">Tip: {p.tip}</p>}
                <div className="post-meta">
                  {p.rating && <span>{'★'.repeat(p.rating)}{'☆'.repeat(5 - p.rating)}</span>}
                  {p.approximateExpense != null && <span>~ Rs. {p.approximateExpense}</span>}
                </div>
                <p className="muted small">by {p.user?.name}</p>
              </div>
            </article>
          ))}
        </div>
      </section>
    </div>
  )
}
