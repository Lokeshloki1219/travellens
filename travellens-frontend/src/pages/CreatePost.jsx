import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api/axios.js'

export default function CreatePost() {
  const navigate = useNavigate()
  const [destinations, setDestinations] = useState([])
  const [destinationId, setDestinationId] = useState('')
  const [caption, setCaption] = useState('')
  const [tip, setTip] = useState('')
  const [expense, setExpense] = useState('')
  const [rating, setRating] = useState(5)
  const [file, setFile] = useState(null)
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [success, setSuccess] = useState('')

  useEffect(() => {
    api.get('/destinations').then((res) => {
      setDestinations(res.data)
      if (res.data.length > 0) setDestinationId(res.data[0].id)
    })
  }, [])

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setSuccess('')
    setSubmitting(true)

    const formData = new FormData()
    formData.append('destinationId', destinationId)
    formData.append('caption', caption)
    formData.append('tip', tip)
    if (expense) formData.append('approximateExpense', expense)
    formData.append('rating', rating)
    if (file) formData.append('media', file)

    try {
      await api.post('/posts', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      setSuccess('Your experience is live. Thanks for sharing!')
      setCaption('')
      setTip('')
      setExpense('')
      setFile(null)
      setTimeout(() => navigate(`/destinations/${destinationId}`), 900)
    } catch (err) {
      setError(err.response?.data?.message || 'Could not publish your post.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="page page-narrow">
      <h1>Share a trip</h1>
      <p className="muted">Add a photo or short video, a tip, and roughly what it cost you.</p>

      <form className="form-card" onSubmit={handleSubmit}>
        <label>
          Destination
          <select value={destinationId} onChange={(e) => setDestinationId(e.target.value)} required>
            {destinations.map((d) => (
              <option key={d.id} value={d.id}>
                {d.name}
              </option>
            ))}
          </select>
        </label>

        <label>
          Caption
          <textarea value={caption} onChange={(e) => setCaption(e.target.value)} rows={3} required />
        </label>

        <label>
          A practical tip
          <input type="text" value={tip} onChange={(e) => setTip(e.target.value)} placeholder="e.g. Book the sunset slot, not sunrise" />
        </label>

        <label>
          Approximate expense (optional)
          <input type="number" value={expense} onChange={(e) => setExpense(e.target.value)} min="0" />
        </label>

        <label>
          Rating
          <select value={rating} onChange={(e) => setRating(Number(e.target.value))}>
            {[5, 4, 3, 2, 1].map((n) => (
              <option key={n} value={n}>
                {n} star{n > 1 ? 's' : ''}
              </option>
            ))}
          </select>
        </label>

        <label>
          Photo or short video (optional)
          <input type="file" accept="image/*,video/*" onChange={(e) => setFile(e.target.files[0])} />
        </label>

        {error && <p className="form-error">{error}</p>}
        {success && <p className="form-note">{success}</p>}

        <button type="submit" className="btn btn-primary" disabled={submitting || !destinationId}>
          {submitting ? 'Publishing…' : 'Publish'}
        </button>
      </form>
    </div>
  )
}
