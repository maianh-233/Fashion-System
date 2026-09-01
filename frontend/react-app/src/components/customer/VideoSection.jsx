export default function VideoSection() {
  return (
    <section className="home-section home-story">
      
      <div className="relative mx-auto w-full max-w-5xl rounded-2xl overflow-hidden border border-zinc-700 bg-zinc-800 aspect-video">
        <iframe
          width="100%"
          height="100%"
          src="https://www.youtube.com/embed/Ghjuyt26v60"
          title="Lunaria Fashion Story"
          frameBorder="0"
          allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
          allowFullScreen
          className="absolute inset-0"
        />
      </div>
      
      <p className="mt-4 text-center text-sm leading-6 text-zinc-400 max-w-xl mx-auto">
        Khám phá câu chuyện đằng sau những thiết kế thanh lịch của Lunaria - nơi sáng tạo gặp gỡ truyền thống.
      </p>
    </section>
  );
}
