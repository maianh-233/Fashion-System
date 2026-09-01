import { BadgeCheck, Quote, Star } from "lucide-react";

function ExpertCard({ expert }) {
  return (
    <article className="home-expert-card">
      <div className="home-expert-card__topline">
        <Quote size={22} aria-hidden="true" />
        <div className="home-expert-card__rating" aria-label="Đánh giá 5 trên 5 sao">
          {Array.from({ length: 5 }, (_, index) => <Star key={index} size={12} fill="currentColor" />)}
        </div>
      </div>

      <blockquote>“{expert.quote}”</blockquote>

      <div className="home-expert-card__person">
        <span className="home-expert-card__avatar" aria-hidden="true">{expert.avatar}</span>
        <span>
          <strong>{expert.name} <BadgeCheck size={14} aria-label="Đã xác minh" /></strong>
          <small>{expert.title}</small>
        </span>
      </div>

      <div className="home-expert-card__meta">
        <span>{expert.specialty}</span>
        <span>{expert.experience}</span>
      </div>
    </article>
  );
}

export default function ExpertSection({ experts }) {
  return (
    <section className="home-section home-experts" aria-labelledby="experts-title">
      <div className="home-section__heading">
        <div>
          <p>Professional voices</p>
          <h2 id="experts-title">Góc nhìn từ chuyên gia</h2>
        </div>
        <span>Những đánh giá độc lập về thiết kế và trải nghiệm Lunaria</span>
      </div>

      <div className="home-experts__grid">
        {experts.map((expert) => <ExpertCard key={expert.id} expert={expert} />)}
      </div>
    </section>
  );
}
