import { Link } from 'react-router-dom';
import { useAppData } from '../context/AppDataContext';
import { formatShortDate } from '../lib/utils';

const categories = ['Tech Conferences', 'Workshops', 'Community Meetups', 'Career Sessions'];
const categoryFeatures = [
  { title: 'Live talks', icon: 'LT', text: 'Hear fresh ideas from speakers, founders, and teams building real products.' },
  { title: 'Hands-on sessions', icon: 'HS', text: 'Join guided workshops where attendees learn by doing, not just watching.' },
  { title: 'Fun atmosphere', icon: 'FA', text: 'Expect music, social breaks, and a welcoming setup that keeps the day lively.' },
  { title: 'Networking', icon: 'NW', text: 'Meet future collaborators, mentors, hiring teams, and people with shared interests.' },
];

export default function HomePage() {
  const { events } = useAppData();
  const trendingEvents = events.slice(0, 4);

  return (
    <div className="space-y-10">
      <section className="hero-shell overflow-hidden p-0">
        <div className="hero-bright relative px-8 py-16 sm:px-10 lg:px-14 lg:py-20">
          <div className="hero-bright-orb hero-bright-orb-left" />
          <div className="hero-bright-orb hero-bright-orb-right" />
          <div className="relative max-w-3xl space-y-5">
            <span className="hero-pill-dark">EventsNow experiences</span>
            <h1 className="font-display text-5xl leading-none text-white sm:text-6xl">
              Discover events that feel worth showing up for.
            </h1>
            <p className="max-w-2xl text-base leading-8 text-white/90">
              From community meetups to high-energy workshops, EventsNow helps people find events, connect with others,
              and enjoy the full experience from registration to attendance.
            </p>
            <div className="flex flex-wrap gap-3">
              <Link to="/events" className="btn-primary">
                Explore Events
              </Link>
              <Link to="/auth" className="btn-light">
                Join EventsNow
              </Link>
            </div>
          </div>
        </div>

        <div className="p-8 sm:p-10 lg:p-14">
          <div className="space-y-6">
            <span className="hero-pill">EventsNow platform</span>
            <h2 className="max-w-3xl font-display text-4xl leading-none text-slate-950 sm:text-5xl">
              A smoother place to browse, organize, and manage real event experiences.
            </h2>
            <p className="max-w-2xl text-base leading-8 text-slate-600">
              Explore what is coming next, manage event operations with confidence, and give attendees a smoother
              registration experience from the first click to the confirmed ticket.
            </p>
          </div>
        </div>
      </section>

      <section className="space-y-5">
        <div className="flex items-end justify-between gap-4">
          <div>
            <p className="section-kicker">Trending now</p>
            <h2 className="section-title">Featured events</h2>
          </div>
          <Link to="/events" className="text-sm font-semibold text-slate-700 transition hover:text-slate-950">
            View all events
          </Link>
        </div>
        <div className="grid gap-4 lg:grid-cols-4">
          {trendingEvents.length ? (
            trendingEvents.map((event) => (
              <article key={event.id} className="glass-panel p-5">
                <p className="text-sm font-semibold text-slate-900">{event.title}</p>
                <p className="mt-2 text-sm text-slate-500">{formatShortDate(event.startTime)}</p>
                <p className="mt-4 line-clamp-3 text-sm leading-6 text-slate-600">{event.description}</p>
                <Link to={`/events/${event.id}`} className="mt-5 inline-flex text-sm font-semibold text-slate-800">
                  See details
                </Link>
              </article>
            ))
          ) : (
            <div className="glass-panel p-6 text-sm text-slate-500 lg:col-span-4">
              No published events yet. Admins can start by creating the first event from System Administration.
            </div>
          )}
        </div>
      </section>

      <section className="space-y-5">
        <div>
          <p className="section-kicker">Having variety of types</p>
          <h2 className="section-title">Event categories</h2>
        </div>
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          {categories.map((category) => (
            <article key={category} className="category-card">
              <p className="text-lg font-semibold text-slate-950">{category}</p>
              <p className="mt-3 text-sm leading-6 text-slate-600">
                Curated sessions designed to help attendees find relevant experiences faster.
              </p>
            </article>
          ))}
        </div>
      </section>

      <section className="space-y-5">
        <div>
          <p className="section-kicker">What to expect</p>
          <h2 className="section-title">Fun, learning, and networking built into the experience</h2>
        </div>
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          {categoryFeatures.map((item) => (
            <article key={item.title} className="interactive-card">
              <div className="feature-icon">{item.icon}</div>
              <p className="mt-4 text-lg font-semibold text-slate-950">{item.title}</p>
              <p className="mt-3 text-sm leading-6 text-slate-600">{item.text}</p>
            </article>
          ))}
        </div>
      </section>
    </div>
  );
}
