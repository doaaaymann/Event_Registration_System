import { useState } from 'react';

const faqSections = [
  {
    title: 'For Participants',
    items: [
      {
        question: 'How do I register for an event?',
        answer: 'Open Find Events, choose an upcoming event, and use the Register button. Confirmed registrations then appear in My Tickets.',
      },
      {
        question: 'Can I register for rescheduled events?',
        answer: 'Yes. Both SCHEDULED and RESCHEDULED events can accept registrations as long as seats are still available.',
      },
      {
        question: 'Where can I see my joined events?',
        answer: 'Use My Tickets to review the events you successfully registered for and quickly open their details again.',
      },
      {
        question: 'Why can the Register button be disabled?',
        answer: 'It stays disabled when the event is full, cancelled, you already registered, or your account is not a participant account.',
      },
    ],
  },
  {
    title: 'For Organizers',
    items: [
      {
        question: 'How do I know which events belong to me?',
        answer: 'Your Organizer Panel only shows events where your account is assigned as one of the organizers.',
      },
      {
        question: 'Can one event have more than one organizer?',
        answer: 'Yes. Events can include multiple organizers, and every assigned organizer can access that event from the organizer side.',
      },
      {
        question: 'Can organizers view registered participants?',
        answer: 'Yes. Any organizer assigned to that event, plus admins, can open the event details page and review participants.',
      },
      {
        question: 'Will organizers receive updates about schedule changes?',
        answer: 'Yes. Organizers receive assignment and schedule-related notifications inside the app so they stay aligned on what changed.',
      },
    ],
  },
  {
    title: 'Using Eventify',
    items: [
      {
        question: 'What happens after an event is rescheduled?',
        answer: 'The updated time appears across the app, and the event can still accept participant registrations if seats remain available.',
      },
      {
        question: 'Where should I check for updates?',
        answer: 'Participants can use Notifications and My Tickets, while organizers can use Notifications and the Organizer Panel.',
      },
      {
        question: 'Do I need an account to browse events?',
        answer: 'No. Browsing is open, but signing in as a participant is required before registration can be completed.',
      },
    ],
  },
];

export default function HelpCenterPage() {
  const [openKey, setOpenKey] = useState('For Participants-0');

  return (
    <div className="space-y-8">
      <section className="hero-shell p-8">
        <p className="section-kicker">Help center</p>
        <h1 className="mt-2 font-display text-5xl leading-none text-slate-950">Answers for participants and organizers.</h1>
        <p className="mt-4 max-w-2xl text-base leading-8 text-slate-600">
          A larger FAQ focused on the people actively discovering, joining, and managing events.
        </p>
      </section>

      <div className="space-y-6">
        {faqSections.map((section) => (
          <section key={section.title} className="glass-panel p-6">
            <h2 className="text-2xl font-semibold text-slate-950">{section.title}</h2>
            <div className="mt-5 space-y-3">
              {section.items.map((item, index) => {
                const key = `${section.title}-${index}`;
                const isOpen = openKey === key;

                return (
                  <article key={key} className="faq-item">
                    <button
                      type="button"
                      onClick={() => setOpenKey(isOpen ? '' : key)}
                      className="faq-question"
                    >
                      <span>{item.question}</span>
                      <span className="faq-plus">{isOpen ? '-' : '+'}</span>
                    </button>
                    {isOpen ? <p className="mt-3 text-sm leading-7 text-slate-600">{item.answer}</p> : null}
                  </article>
                );
              })}
            </div>
          </section>
        ))}
      </div>
    </div>
  );
}
