import { Link } from 'react-router-dom';

const roles = [
  {
    title: 'Participant',
    text: 'Browse upcoming events, register in a few clicks, and keep track of your own notifications.',
  },
  {
    title: 'Organizer',
    text: 'Create events, manage seat limits, review attendees, and keep schedules current.',
  },
  {
    title: 'Admin',
    text: 'Create organizers and keep the system ready for real event operations across the full backend stack.',
  },
];

export default function HomePage() {
  return (
    <div className="space-y-8">
      <section className="panel overflow-hidden p-8 sm:p-10">
        <div className="grid gap-10 lg:grid-cols-[1.4fr_0.9fr] lg:items-center">
          <div className="space-y-5">
            <span className="pill">Spring Boot microservices, simplified</span>
            <h1 className="max-w-3xl text-4xl font-semibold tracking-tight text-slate-900 sm:text-5xl">
              A clean front end for registration, scheduling, and event updates.
            </h1>
            <p className="max-w-2xl text-base leading-7 text-slate-600">
              This interface sits on top of your existing gateway APIs and gives admins, organizers, and participants
              a calmer way to use the project without the usual student-dashboard feel.
            </p>
            <div className="flex flex-wrap gap-3">
              <Link to="/login" className="btn-primary">
                Sign in
              </Link>
              <Link to="/register" className="btn-secondary">
                Register as participant
              </Link>
            </div>
          </div>

          <div className="panel-soft p-6">
            <p className="text-sm font-semibold text-slate-900">What this frontend supports</p>
            <div className="mt-5 space-y-4">
              <div className="rounded-2xl bg-white px-4 py-4">
                <p className="text-sm font-medium text-slate-900">Event management</p>
                <p className="mt-1 text-sm text-slate-500">Create, edit, reschedule, cancel, and monitor seat usage.</p>
              </div>
              <div className="rounded-2xl bg-white px-4 py-4">
                <p className="text-sm font-medium text-slate-900">Participant flow</p>
                <p className="mt-1 text-sm text-slate-500">Register, view your registrations, and read notifications.</p>
              </div>
              <div className="rounded-2xl bg-white px-4 py-4">
                <p className="text-sm font-medium text-slate-900">Role-based access</p>
                <p className="mt-1 text-sm text-slate-500">Admin, organizer, and participant screens from the same app.</p>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="grid gap-4 md:grid-cols-3">
        {roles.map((role) => (
          <article key={role.title} className="panel p-6">
            <p className="text-lg font-semibold text-slate-900">{role.title}</p>
            <p className="mt-3 text-sm leading-6 text-slate-500">{role.text}</p>
          </article>
        ))}
      </section>
    </div>
  );
}
