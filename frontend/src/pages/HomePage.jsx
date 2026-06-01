import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { getProducts } from "../api/catalogApi.js";
import BookCard from "../components/BookCard.jsx";
import { normalizeBook, pageRows } from "../utils/mappers.js";

export default function HomePage() {
  const { books, message } = useCatalog();
  const featured = books.slice(0, 4);
  const orbitBooks = featured.concat(books.slice(4, 9));
  const [activeOrbit, setActiveOrbit] = useState(0);

  useEffect(() => {
    if (orbitBooks.length === 0) return undefined;
    const timer = setInterval(() => {
      setActiveOrbit((current) => (current + 1) % orbitBooks.length);
    }, 1600);
    return () => clearInterval(timer);
  }, [orbitBooks.length]);

  return (
    <>
      <section id="hero">
        <div className="h-atmo" />
        <div className="h-l">
          <div className="h-tag"><span />Aivira single-vendor bookstore</div>
          <div className="hero-title">
            <span>EVERY</span>
            <span>BOOK A</span>
            <span className="blue">WORLD</span>
            <em>waiting to be explored.</em>
          </div>
          <p className="h-desc">Aivira curates books across business, growth, fiction, technology, and education with a checkout flow connected to the bookstore backend.</p>
          <div className="h-btns">
            <Link className="btn-fill" to="/category/all">Explore Books</Link>
            <Link className="btn-line" to="/cart">View Cart</Link>
          </div>
          <div className="h-stats">
            <div><strong>{books.length}+</strong><span>Titles</span></div>
            <div><strong>COD</strong><span>Payments</span></div>
            <div><strong>VNPay</strong><span>MoMo</span></div>
          </div>
        </div>
        <div className="h-r">
          {orbitBooks.map((book, index) => (
            <Link className={`fb ${orbitClass(index, activeOrbit, orbitBooks.length)}`} key={book.id} to={`/product/${book.slug}`} onMouseEnter={() => setActiveOrbit(index)}>
              <img src={book.image} alt={book.title} />
            </Link>
          ))}
        </div>
      </section>
      <section className="ticker"><div>Business and Finance - Self-help and Growth - Literature and Fiction - Skills and Wellness - Technology - Education</div></section>
      <CategoryShowcase />
      <section id="featured">
        <SectionHead chip="Backend catalog" title="THIS WEEK'S PICKS" link="/category/all" />
        {message && <div className="notice page-notice">{message}</div>}
        <div className="bks-g">{featured.map((book) => <BookCard key={book.id} book={book} />)}</div>
      </section>
      <HorizontalStrip />
      <QuoteSection />
      <section id="shop-main">
        <SectionHead chip="Collection" title="ALL BOOKS" link="/category/all" />
        <BookGrid books={books.slice(0, 12)} />
      </section>
      <HowItWorks />
      <section id="about">
        <div className="ab-vis whale-banner">
          <StarField count={90} small />
          <div className="wb-logo">AIVIRA</div>
          <div className="wb-sep" />
          <div className="wb-quote">"Unlock your new chapters"</div>
          <div className="wb-badge-box"><strong>12K+</strong><span>Happy Readers</span></div>
        </div>
        <div>
          <div className="sec-chip">Why Us</div>
          <h2 className="sec-title">MORE THAN A STORE</h2>
          <p className="sec-desc">Aivira is the sole bookstore operator. Customers browse, add books to cart, checkout, pay, and track orders in one place.</p>
          <ul className="ab-list">
            <li>Admin-managed catalog with book variants and stock.</li>
            <li>Customer cart, checkout, COD, VNPay, and MoMo flows.</li>
            <li>Frontend uses real backend APIs for auth, catalog, cart, checkout, orders, and admin.</li>
          </ul>
        </div>
      </section>
    </>
  );
}

function useCatalog(params = {}) {
  const [books, setBooks] = useState([]);
  const [message, setMessage] = useState("");

  useEffect(() => {
    let alive = true;
    setMessage("");
    getProducts({ page: 1, size: 50, ...params })
      .then((page) => {
        const rows = pageRows(page);
        if (!alive) return;
        setBooks(rows.map((row) => normalizeBook(row)));
      })
      .catch((error) => {
        if (alive) {
          setBooks([]);
          setMessage(error.message || "Could not load backend catalog.");
        }
      });
    return () => {
      alive = false;
    };
  }, [params.keyword, params.categorySlug]);

  return { books, message };
}

function orbitClass(index, active, length) {
  let diff = index - active;
  if (diff < -Math.floor(length / 2)) diff += length;
  if (diff > Math.floor(length / 2)) diff -= length;
  if (diff === 0) return "fb-active";
  if (diff === 1) return "fb-right";
  if (diff === -1) return "fb-left";
  if (diff === 2) return "fb-far-right";
  if (diff === -2) return "fb-far-left";
  return "fb-hidden";
}

function CategoryShowcase() {
  const cards = [
    ["business", "01", "BUSINESS & FINANCE", "120+ titles", "https://images.unsplash.com/photo-1507842217343-583bb7270b66?q=80&w=900&auto=format&fit=crop"],
    ["self-help", "02", "SELF-HELP & GROWTH", "180+ titles", "https://images.unsplash.com/photo-1519682337058-a94d519337bc?q=80&w=900&auto=format&fit=crop"],
    ["literature", "03", "LITERATURE & FICTION", "95+ titles", "https://images.unsplash.com/photo-1457369804613-52c61a468e7d?q=80&w=900&auto=format&fit=crop"],
    ["skills", "04", "SKILLS & WELLNESS", "110+ titles", "https://images.unsplash.com/photo-1499750310107-5fef28a66643?q=80&w=900&auto=format&fit=crop"]
  ];

  return (
    <section id="categories-showcase">
      <SectionHead chip="Explore" title="BOOK CATEGORIES" link="/category/all" />
      <div className="cat-grid">
        {cards.map(([id, number, title, count, image]) => (
          <Link className="cat-c" to={`/category/${id}`} key={id}>
            <div className="cat-bg" style={{ backgroundImage: `url("${image}")` }} />
            <div className="cat-top"><span>{number}</span></div>
            <div className="cat-bot">
              <h3>{title}</h3>
              <small>{count}</small>
              <b>-&gt;</b>
            </div>
          </Link>
        ))}
      </div>
    </section>
  );
}

function HorizontalStrip() {
  const slides = [
    ["CATEGORY 01", "01", "Business & Finance", "BUILD YOUR EMPIRE", "From startups to Wall Street - books that shape modern business."],
    ["CATEGORY 02", "02", "Self-help & Growth", "UNLOCK YOUR BEST SELF", "Habits, mindset, discipline - tools for becoming who you want to be."],
    ["CATEGORY 03", "03", "Literature & Fiction", "STORIES THAT LAST", "Timeless narratives from great minds across generations."],
    ["CATEGORY 04", "04", "Skills & Wellness", "MASTER EVERY DAY", "Communication, health, focus - practical tools for a better life."],
    ["EXPLORE MORE", "+", "Aivira", "500+ TITLES", "New arrivals every week for your next great read."]
  ];
  return (
    <div id="hscroll">
      <div className="hs-track">
        {slides.map((slide, index) => (
          <div className="hs-slide" key={index}>
            <div className="hs-label">{slide[0]}</div>
            <div className="hs-num">{slide[1]}</div>
            <div className="hs-c">
              <div className="hs-cat">{slide[2]}</div>
              <h3>{slide[3]}</h3>
              <p>{slide[4]}</p>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

function QuoteSection() {
  return (
    <section id="quote">
      <div className="q-grid" />
      <div className="q-mark">"</div>
      <p>A book is not just read - it is <em>lived</em>. Aivira exists to help you find the ones that change everything.</p>
      <div className="q-by">- The Aivira Philosophy -</div>
    </section>
  );
}

function HowItWorks() {
  return (
    <section id="how-it-works">
      <div className="hiw-grid-bg" />
      <div className="hiw-inner">
        <div className="hiw-heading">
          <div className="sec-chip">The Process</div>
          <h2 className="sec-title">HOW IT WORKS</h2>
          <p>Three simple steps to your next great read</p>
        </div>
        <div className="hiw-cards">
          {[
            ["01", "Discover", "Browse curated collections across business, growth, literature, education, and skills."],
            ["02", "Choose", "Read summaries, metadata, prices, stock state, and recommendations before checkout."],
            ["03", "Buy & Track", "Add to cart, checkout with COD/VNPay/MoMo, then follow your order status."]
          ].map((item) => (
            <div className="hiw-card" key={item[0]}>
              <div className="hiw-num">{item[0]}</div>
              <h3>{item[1]}</h3>
              <p>{item[2]}</p>
              <div className="hiw-accent" />
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

function StarField({ count = 80, small = false }) {
  const stars = useMemo(() => Array.from({ length: count }, (_, index) => ({
    id: index,
    left: `${Math.random() * 100}%`,
    top: `${Math.random() * 100}%`,
    size: `${(small ? 0.5 : 0.7) + Math.random() * (small ? 1.2 : 2)}px`,
    delay: `${Math.random() * 5}s`,
    duration: `${2 + Math.random() * 4}s`,
    opacity: 0.16 + Math.random() * 0.58
  })), [count, small]);

  return (
    <div className="stars">
      {stars.map((star) => (
        <i key={star.id} style={{ left: star.left, top: star.top, width: star.size, height: star.size, animationDelay: star.delay, animationDuration: star.duration, opacity: star.opacity }} />
      ))}
    </div>
  );
}

function SectionHead({ chip, title, link }) {
  return <div className="section-head"><div><div className="sec-chip">{chip}</div><h2 className="sec-title">{title}</h2></div>{link && <Link to={link}>View all books</Link>}</div>;
}

function BookGrid({ books }) {
  if (!books.length) return <div className="empty"><h3>No books found</h3></div>;
  return <div className="book-grid">{books.map((book) => <BookCard key={book.id} book={book} />)}</div>;
}
