import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { motion } from "motion/react";

import { getProducts } from "../api/catalogApi.js";
import BookCard from "../components/BookCard.jsx";
import { normalizeBook, pageRows } from "../utils/mappers.js";

const FALLBACK_BOOKS = [
  {
    id: "demo-how-innovation-works",
    slug: "how-innovation-works",
    title: "How Innovation Works",
    author: "Matt Ridley",
    catLabel: "Business",
    price: 150000,
    priceOld: 180000,
    rating: 4.9,
    badge: "Bestseller",
    image:
      "https://images.unsplash.com/photo-1544947950-fa07a98d237f?q=80&w=600&auto=format&fit=crop",
  },
  {
    id: "demo-deep-work",
    slug: "deep-work",
    title: "Deep Work",
    author: "Cal Newport",
    catLabel: "Skills",
    price: 128000,
    rating: 4.8,
    image:
      "https://images.unsplash.com/photo-1519682337058-a94d519337bc?q=80&w=600&auto=format&fit=crop",
  },
  {
    id: "demo-business-strategy",
    slug: "business-strategy",
    title: "Business Strategy",
    author: "Aivira Editorial",
    catLabel: "Business",
    price: 250000,
    rating: 4.9,
    image:
      "https://images.unsplash.com/photo-1512820790803-83ca734da794?q=80&w=600&auto=format&fit=crop",
  },
  {
    id: "demo-night-library",
    slug: "night-library",
    title: "The Night Library",
    author: "Aivira Editorial",
    catLabel: "Literature",
    price: 210000,
    rating: 4.7,
    badge: "New",
    image:
      "https://images.unsplash.com/photo-1524995997946-a1c2e315a42f?q=80&w=600&auto=format&fit=crop",
  },
  {
    id: "demo-focused-work",
    slug: "focused-work-flow",
    title: "Focused Work Flow",
    author: "Aivira Editorial",
    catLabel: "Education",
    price: 140000,
    rating: 4.6,
    image:
      "https://images.unsplash.com/photo-1484480974693-6ca0a78fb36b?q=80&w=600&auto=format&fit=crop",
  },
  {
    id: "demo-code-craft",
    slug: "code-craft",
    title: "Code Craft",
    author: "Aivira Editorial",
    catLabel: "Technology",
    price: 240000,
    rating: 4.8,
    image:
      "https://images.unsplash.com/photo-1515879218367-8466d910aaa4?q=80&w=600&auto=format&fit=crop",
  },
  {
    id: "demo-psychology-money",
    slug: "psychology-of-money",
    title: "The Psychology of Money",
    author: "Morgan Housel",
    catLabel: "Finance",
    price: 165000,
    rating: 4.9,
    image:
      "https://images.unsplash.com/photo-1621351183012-e2f9972dd9bf?q=80&w=600&auto=format&fit=crop",
  },
  {
    id: "demo-moon-reader",
    slug: "moon-reader",
    title: "Moon Reader",
    author: "Aivira Editorial",
    catLabel: "Science",
    price: 160000,
    rating: 4.7,
    image:
      "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?q=80&w=600&auto=format&fit=crop",
  },
];

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
    <div className="tw-home w-full overflow-hidden bg-slate-50">
      <section className="relative flex min-h-screen items-center overflow-hidden bg-slate-950 px-4 pb-16 pt-28 text-white md:px-8">
        <div className="pointer-events-none absolute inset-0 overflow-hidden">
          <div className="absolute -right-1/4 top-1/4 h-[800px] w-[800px] rounded-full bg-blue-600/20 blur-[120px] mix-blend-screen" />
          <div className="absolute -bottom-1/4 -left-1/4 h-[600px] w-[600px] rounded-full bg-sky-400/10 blur-[100px] mix-blend-screen" />
          <div className="absolute inset-0 bg-[url('https://grainy-gradients.vercel.app/noise.svg')] opacity-20 mix-blend-overlay" />
        </div>

        <div className="relative z-10 mx-auto grid w-full max-w-7xl grid-cols-1 items-center gap-16 lg:grid-cols-2">
          <motion.div
            className="z-20 flex flex-col items-start gap-8 pt-10"
            initial={{ opacity: 0, y: 22 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.7 }}
          >
            <motion.div
              initial={{ opacity: 0, scale: 0.9 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ duration: 0.8, ease: "backOut" }}
              className="inline-flex items-center gap-3 rounded-full border border-white/10 bg-white/5 px-4 py-2 backdrop-blur-md"
            >
              <span className="h-2 w-2 animate-pulse rounded-full bg-blue-400" />
              <span className="text-sm font-medium tracking-wide text-slate-300">
                Aivira Single-Vendor Bookstore
              </span>
            </motion.div>

            <motion.h1
              initial={{ opacity: 0, y: 30 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.8, delay: 0.2 }}
              className="font-serif text-5xl font-bold leading-[1.1] tracking-tight md:text-7xl"
            >
              EVERY BOOK <br />
              A{" "}
              <span className="bg-gradient-to-r from-blue-400 to-sky-200 bg-clip-text text-transparent">
                WORLD
              </span>
              <br />
              <em className="mt-2 block font-serif text-3xl font-light italic text-slate-400 md:text-5xl">
                waiting to be explored.
              </em>
            </motion.h1>

            <motion.p
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.8, delay: 0.3 }}
              className="max-w-lg text-lg font-light leading-relaxed text-slate-400 md:text-xl"
            >
              Aivira curates books across business, growth, fiction, technology,
              and education with a checkout flow connected to the bookstore backend.
            </motion.p>

            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.8, delay: 0.4 }}
              className="flex flex-wrap items-center gap-4"
            >
              <Link
                to="/category/all"
                className="rounded-full bg-white px-8 py-4 font-bold tracking-wide text-slate-900 shadow-[0_0_40px_rgba(255,255,255,0.2)] transition-all duration-300 hover:scale-105 hover:bg-slate-200"
              >
                Explore Library
              </Link>
              <Link
                to="/cart"
                className="rounded-full border border-white/20 bg-white/5 px-8 py-4 font-bold tracking-wide backdrop-blur transition-all duration-300 hover:bg-white/10"
              >
                View Cart
              </Link>
            </motion.div>

            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ duration: 1, delay: 0.8 }}
              className="mt-4 grid w-full grid-cols-3 gap-8 border-t border-white/10 pt-8"
            >
              <div>
                <strong className="block font-serif text-2xl">{books.length}+</strong>
                <span className="mt-1 block text-sm uppercase tracking-widest text-slate-500">
                  Titles
                </span>
              </div>
              <div>
                <strong className="block font-serif text-2xl">COD</strong>
                <span className="mt-1 block text-sm uppercase tracking-widest text-slate-500">
                  Supported
                </span>
              </div>
              <div>
                <strong className="block font-serif text-2xl">VNPay</strong>
                <span className="mt-1 block text-sm uppercase tracking-widest text-slate-500">
                  & MoMo
                </span>
              </div>
            </motion.div>
          </motion.div>

          <HeroBookOrbit
            books={orbitBooks}
            activeOrbit={activeOrbit}
            onActivate={setActiveOrbit}
          />
        </div>
      </section>

      <Ticker />

      <CategoryShowcase />

      <section className="bg-white px-4 py-24 md:px-8">
        <div className="mx-auto max-w-7xl">
          <SectionHead chip="Backend Catalog" title="This Week's Picks" link="/category/all" />
          {message && <div className="mt-8 rounded-2xl border border-amber-200 bg-amber-50 px-5 py-4 text-sm font-semibold text-amber-700">{message}</div>}
          <div className="mt-12 grid grid-cols-2 gap-6 md:grid-cols-4">
            {featured.map((book, index) => (
              <motion.div
                key={book.id}
                initial={{ opacity: 0, y: 40 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ duration: 0.6, delay: index * 0.1 }}
              >
                <BookCard book={book} />
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      <QuoteSection />

      <section className="mx-auto max-w-7xl px-4 py-24 md:px-8">
        <SectionHead chip="Collection" title="All Books" link="/category/all" />
        <BookGrid books={books.slice(0, 12)} />
      </section>

      <HowItWorks />

      <AboutSection booksCount={books.length} />

      <LatestNews />
    </div>
  );
}

function useCatalog(params = {}) {
  const [books, setBooks] = useState(FALLBACK_BOOKS);
  const [message, setMessage] = useState("");

  useEffect(() => {
    let alive = true;
    setMessage("");

    getProducts({ page: 1, size: 50, ...params })
      .then((page) => {
        const rows = pageRows(page);
        if (!alive) return;
        const normalized = rows.map((row) => normalizeBook(row));
        setBooks(normalized.length ? normalized : FALLBACK_BOOKS);
      })
      .catch((error) => {
        if (alive) {
          setBooks(FALLBACK_BOOKS);
          setMessage(error.message || "Showing demo catalog while backend is unavailable.");
        }
      });

    return () => {
      alive = false;
    };
  }, [params.keyword, params.categorySlug]);

  return { books, message };
}

function HeroBookOrbit({ books, activeOrbit, onActivate }) {
  const positions = [
    { x: -220, y: -42, rotate: -12, scale: 0.86, z: 10 },
    { x: -88, y: -12, rotate: -5, scale: 0.96, z: 20 },
    { x: 58, y: -28, rotate: 3, scale: 1.08, z: 30 },
    { x: 200, y: 8, rotate: 8, scale: 0.98, z: 20 },
    { x: 332, y: -38, rotate: 13, scale: 0.86, z: 10 },
  ];

  const visibleBooks = useMemo(() => {
    if (!books.length) return [];

    const items = [];
    const max = Math.min(5, books.length);

    for (let index = 0; index < max; index += 1) {
      items.push(books[(activeOrbit + index) % books.length]);
    }

    return items;
  }, [books, activeOrbit]);

  if (!visibleBooks.length) {
    return (
      <div className="relative hidden h-[600px] w-full lg:block">
        <div className="absolute left-1/2 top-1/2 h-[380px] w-[260px] -translate-x-1/2 -translate-y-1/2 rounded-3xl border border-white/10 bg-white/5 shadow-2xl backdrop-blur" />
      </div>
    );
  }

  return (
    <div className="relative hidden h-[600px] w-full lg:block">
      <div className="absolute left-[54%] top-1/2 h-[460px] w-[460px] -translate-x-1/2 -translate-y-1/2 rounded-full bg-blue-600/15 blur-3xl" />
      <div className="absolute bottom-10 left-1/2 h-16 w-[520px] -translate-x-1/2 rounded-full bg-black/40 blur-2xl" />

      {visibleBooks.map((book, index) => {
        const position = positions[index] || positions[positions.length - 1];
        const originalIndex = books.findIndex((item) => item.id === book.id);

        return (
          <motion.div
            key={`${book.id}-${activeOrbit}-${index}`}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{
              duration: 0.55,
              delay: index * 0.08,
              ease: "easeOut",
            }}
            onMouseEnter={() => {
              if (originalIndex >= 0) onActivate(originalIndex);
            }}
            className="absolute left-1/2 top-1/2 aspect-[2/3] w-[190px] cursor-pointer rounded-xl"
            style={{
              zIndex: position.z,
              transform: `translate(calc(-50% + ${position.x}px), calc(-50% + ${position.y}px)) rotate(${position.rotate}deg) scale(${position.scale})`,
            }}
          >
            <Link
              to={`/product/${book.slug}`}
              className="group absolute inset-0 block overflow-hidden rounded-xl border border-white/10 bg-slate-900 shadow-[0_30px_80px_rgba(0,0,0,0.55)] transition duration-500 hover:-translate-y-4 hover:scale-105"
            >
              <img
                src={book.image || book.cover}
                alt={book.title}
                className="h-full w-full object-cover transition duration-700 group-hover:scale-105"
              />

              <div className="absolute inset-0 bg-gradient-to-tr from-slate-950/55 via-transparent to-white/20 opacity-80" />
              <div className="absolute bottom-0 left-0 right-0 p-4 opacity-0 transition-opacity duration-300 group-hover:opacity-100">
                <p className="line-clamp-2 font-serif text-lg font-bold leading-tight text-white">
                  {book.title}
                </p>
                <p className="mt-1 line-clamp-1 text-xs font-medium text-slate-300">
                  {book.author}
                </p>
              </div>
            </Link>
          </motion.div>
        );
      })}
    </div>
  );
}

function Ticker() {
  const text = "Business and Finance - Self-help and Growth - Literature and Fiction - Skills and Wellness - Technology - Education - ";

  return (
    <div className="flex overflow-hidden whitespace-nowrap border-y border-white/5 bg-slate-900 py-4">
      <motion.div
        animate={{ x: ["0%", "-50%"] }}
        transition={{ ease: "linear", duration: 25, repeat: Infinity }}
        className="flex items-center space-x-16 text-sm font-bold uppercase tracking-[0.3em] text-slate-500"
      >
        {Array.from({ length: 4 }).map((_, index) => (
          <span key={index}>{text}</span>
        ))}
      </motion.div>
    </div>
  );
}

function CategoryShowcase() {
  const cards = [
    {
      id: "business",
      title: "Business",
      count: "120+ titles",
      image: "https://images.unsplash.com/photo-1507842217343-583bb7270b66?q=80&w=600&auto=format&fit=crop",
    },
    {
      id: "self-help",
      title: "Self-help",
      count: "180+ titles",
      image: "https://images.unsplash.com/photo-1519682337058-a94d519337bc?q=80&w=600&auto=format&fit=crop",
    },
    {
      id: "literature",
      title: "Literature",
      count: "95+ titles",
      image: "https://images.unsplash.com/photo-1457369804613-52c61a468e7d?q=80&w=600&auto=format&fit=crop",
    },
    {
      id: "skills",
      title: "Wellness",
      count: "110+ titles",
      image: "https://images.unsplash.com/photo-1499750310107-5fef28a66643?q=80&w=600&auto=format&fit=crop",
    },
  ];

  return (
    <section className="mx-auto max-w-7xl px-4 py-24 md:px-8">
      <SectionHead chip="Explore" title="Curated Collections" link="/category/all" />
      <div className="mt-12 grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-4">
        {cards.map((category, index) => (
          <motion.div
            key={category.id}
            initial={{ opacity: 0, y: 30 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true, margin: "-50px" }}
            transition={{ duration: 0.6, delay: index * 0.1 }}
          >
            <Link
              to={`/category/${category.id}`}
              className="group relative block aspect-[4/5] w-full overflow-hidden rounded-2xl bg-slate-200 shadow-sm transition-all duration-500 hover:shadow-2xl"
            >
              <img
                src={category.image}
                alt={category.title}
                className="h-full w-full object-cover transition-transform duration-700 group-hover:scale-110"
              />
              <div className="absolute inset-0 bg-gradient-to-b from-black/20 via-black/10 to-black/80" />
              <div className="absolute left-6 top-6 z-10 flex h-10 w-10 items-center justify-center rounded-full border border-white/30 font-serif font-bold italic text-white backdrop-blur-md transition-transform duration-500 group-hover:scale-110">
                0{index + 1}
              </div>
              <div className="absolute bottom-6 left-6 right-6 z-10 transition-transform duration-500 group-hover:-translate-y-2">
                <h3 className="mb-1 font-serif text-2xl font-bold text-white">{category.title}</h3>
                <div className="flex items-center justify-between">
                  <p className="text-sm tracking-wide text-white/70">{category.count}</p>
                  <span className="flex h-8 w-8 -translate-x-4 items-center justify-center rounded-full bg-white text-slate-900 opacity-0 transition-all duration-300 group-hover:translate-x-0 group-hover:opacity-100">
                    <span className="text-lg leading-none">-&gt;</span>
                  </span>
                </div>
              </div>
            </Link>
          </motion.div>
        ))}
      </div>
    </section>
  );
}

function QuoteSection() {
  return (
    <section className="relative flex items-center justify-center overflow-hidden bg-blue-900 px-4 py-32 text-white">
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_center,_white_1px,_transparent_1px)] opacity-10 [background-size:24px_24px]" />
      <motion.div
        initial={{ opacity: 0, scale: 0.95 }}
        whileInView={{ opacity: 1, scale: 1 }}
        viewport={{ once: true }}
        transition={{ duration: 1 }}
        className="relative z-10 mx-auto max-w-4xl text-center"
      >
        <div className="mb-8 inline-block h-10 overflow-hidden font-serif text-8xl leading-none text-blue-400/30">"</div>
        <p className="mb-8 font-serif text-3xl font-medium leading-tight md:text-5xl">
          A book is not just read - it is <em className="italic text-blue-200">lived</em>. Aivira exists to help you find the ones that change everything.
        </p>
        <div className="text-sm font-bold uppercase tracking-[0.3em] text-blue-300">
          - The Aivira Philosophy -
        </div>
      </motion.div>
    </section>
  );
}

function BookGrid({ books }) {
  if (!books.length) {
    return (
      <div className="mt-12 rounded-3xl border border-slate-200 bg-white px-8 py-16 text-center">
        <h3 className="font-serif text-2xl font-bold text-slate-900">No books found</h3>
      </div>
    );
  }

  return (
    <div className="mt-12 grid grid-cols-2 gap-x-6 gap-y-12 md:grid-cols-3 lg:grid-cols-4">
      {books.map((book, index) => (
        <motion.div
          key={book.id}
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: "0px 0px -50px 0px" }}
          transition={{ duration: 0.5, delay: (index % 4) * 0.1 }}
        >
          <BookCard book={book} />
        </motion.div>
      ))}
    </div>
  );
}

function HowItWorks() {
  const steps = [
    {
      num: "01",
      title: "Discover",
      desc: "Browse curated collections across business, growth, literature, education, and skills.",
    },
    {
      num: "02",
      title: "Choose",
      desc: "Read summaries, metadata, prices, stock state, and recommendations before checkout.",
    },
    {
      num: "03",
      title: "Buy & Track",
      desc: "Add to cart, checkout with COD/VNPay/MoMo, then follow your order status.",
    },
  ];

  return (
    <section className="mt-12 rounded-t-[3rem] bg-slate-900 px-4 py-24 text-white md:px-8">
      <div className="mx-auto max-w-7xl">
        <div className="mx-auto mb-16 max-w-2xl text-center">
          <div className="mb-4 inline-block rounded-full border border-blue-500/20 bg-blue-500/10 px-3 py-1 text-xs font-bold uppercase tracking-wider text-blue-400">
            The Process
          </div>
          <h2 className="mb-4 font-serif text-4xl font-bold md:text-5xl">How It Works</h2>
          <p className="text-slate-400">Three simple steps to your next great read</p>
        </div>

        <div className="relative grid grid-cols-1 gap-8 md:grid-cols-3">
          <div className="absolute left-[15%] right-[15%] top-[40px] hidden h-px bg-gradient-to-r from-transparent via-blue-500/30 to-transparent md:block" />

          {steps.map((item, index) => (
            <motion.div
              key={item.num}
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.6, delay: index * 0.2 }}
              className="group relative rounded-2xl border border-white/5 bg-slate-800/50 p-8 backdrop-blur transition-colors hover:border-blue-500/30"
            >
              <div className="relative z-10 mx-auto mb-6 flex h-20 w-20 items-center justify-center rounded-full border border-white/10 bg-slate-900 font-serif text-3xl font-bold text-white transition-all duration-500 group-hover:scale-110 group-hover:border-blue-500/50 md:mx-0">
                {item.num}
              </div>
              <h3 className="mb-3 text-center text-2xl font-bold md:text-left">{item.title}</h3>
              <p className="text-center leading-relaxed text-slate-400 md:text-left">{item.desc}</p>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  );
}

function AboutSection({ booksCount }) {
  return (
    <section className="relative overflow-hidden border-t border-slate-800 bg-slate-900 px-4 py-24 text-white md:px-8">
      <div className="absolute inset-0 bg-[url('https://grainy-gradients.vercel.app/noise.svg')] opacity-10 mix-blend-overlay" />
      <div className="pointer-events-none absolute right-0 top-0 h-full w-1/2 bg-gradient-to-l from-blue-900/20 to-transparent" />

      <div className="mx-auto grid max-w-7xl grid-cols-1 items-center gap-16 md:grid-cols-2">
        <motion.div
          initial={{ opacity: 0, x: -40 }}
          whileInView={{ opacity: 1, x: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.8 }}
          className="relative z-10"
        >
          <div className="mb-6 inline-block rounded-full border border-white/20 bg-white/10 px-3 py-1 text-xs font-bold uppercase tracking-wider text-white">
            Why Us
          </div>
          <h2 className="mb-6 font-serif text-4xl font-bold md:text-5xl">AIVIRA BOOKSTORE</h2>
          <p className="mb-8 text-lg font-light leading-relaxed text-slate-300">
            Aivira is the sole bookstore operator. Customers browse, add books to cart, checkout, pay, and track orders in one place.
          </p>

          <ul className="mb-8 space-y-4">
            {[
              "Admin-managed catalog with book variants and stock.",
              "Customer cart, checkout, COD, VNPay, and MoMo flows.",
              "Frontend uses real backend APIs for auth, catalog, cart, checkout, orders, and admin.",
            ].map((item) => (
              <li key={item} className="flex items-center gap-3 font-medium text-slate-300">
                <div className="flex h-6 w-6 items-center justify-center rounded-full border border-blue-500/30 bg-blue-500/20 text-blue-400">
                  <span className="text-xs font-bold leading-none">OK</span>
                </div>
                {item}
              </li>
            ))}
          </ul>

          <Link
            to="/about"
            className="inline-block rounded-full bg-blue-600 px-8 py-4 font-bold tracking-wide text-white shadow-lg shadow-blue-500/25 transition-colors hover:bg-blue-500"
          >
            Learn More
          </Link>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, scale: 0.9 }}
          whileInView={{ opacity: 1, scale: 1 }}
          viewport={{ once: true }}
          transition={{ duration: 0.8 }}
          className="relative"
        >
          <div className="relative aspect-square overflow-hidden rounded-3xl shadow-2xl md:aspect-[4/5]">
            <img
              src="https://images.unsplash.com/photo-1521587760476-6c12a4b040da?q=80&w=800&auto=format&fit=crop"
              alt="Aivira Library"
              className="h-full w-full object-cover"
            />
            <div className="absolute inset-0 bg-gradient-to-tr from-slate-900/80 to-transparent" />
            <div className="absolute bottom-8 left-8 right-8 flex items-center justify-around rounded-2xl border border-white/20 bg-white/10 p-6 text-center shadow-xl backdrop-blur-md">
              <div>
                <strong className="mb-1 block font-serif text-3xl text-white">12K+</strong>
                <span className="text-xs font-bold uppercase tracking-widest text-slate-300">Readers</span>
              </div>
              <div className="h-12 w-px bg-white/20" />
              <div>
                <strong className="mb-1 block font-serif text-3xl text-white">{booksCount}+</strong>
                <span className="text-xs font-bold uppercase tracking-widest text-slate-300">Titles</span>
              </div>
            </div>
          </div>
        </motion.div>
      </div>
    </section>
  );
}

function LatestNews() {
  const posts = [
    {
      title: "Top 10 Books Worth Reading This Year",
      category: "Business",
      date: "02 Jun, 2026",
      image: "https://images.unsplash.com/photo-1542361345-89e58247f2d5?q=80&w=600&auto=format&fit=crop",
    },
    {
      title: "Time Management Lessons From High-Performing Leaders",
      category: "Skills",
      date: "28 May, 2026",
      image: "https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?q=80&w=600&auto=format&fit=crop",
    },
    {
      title: "Why Fiction Helps Readers Build Empathy",
      category: "Literature",
      date: "20 May, 2026",
      image: "https://images.unsplash.com/photo-1474932430478-367d16b99031?q=80&w=600&auto=format&fit=crop",
    },
  ];

  return (
    <section className="border-t border-slate-200 bg-slate-50 px-4 py-24 md:px-8">
      <div className="mx-auto max-w-7xl">
        <SectionHead chip="Insights" title="News & Blog" link="/blog" />
        <div className="mt-12 grid grid-cols-1 gap-8 md:grid-cols-3">
          {posts.map((post, index) => (
            <motion.article
              key={post.title}
              initial={{ opacity: 0, y: 30 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ duration: 0.6, delay: index * 0.15 }}
              className="group flex h-full cursor-pointer flex-col rounded-2xl border border-slate-100 bg-white p-4 transition-all duration-300 hover:shadow-xl"
            >
              <div className="mb-5 aspect-[16/10] w-full overflow-hidden rounded-xl">
                <img
                  src={post.image}
                  alt={post.title}
                  className="h-full w-full object-cover transition-transform duration-700 group-hover:scale-105"
                />
              </div>
              <div className="mb-3 flex items-center gap-3 text-xs font-medium text-slate-500">
                <span className="rounded bg-blue-50 px-2 py-1 font-bold uppercase tracking-wider text-blue-600">
                  {post.category}
                </span>
                <span className="h-1 w-1 rounded-full bg-slate-300" />
                <span>{post.date}</span>
              </div>
              <h3 className="line-clamp-2 font-serif text-lg font-bold leading-snug text-slate-900 transition-colors group-hover:text-blue-600">
                {post.title}
              </h3>
            </motion.article>
          ))}
        </div>
      </div>
    </section>
  );
}

function SectionHead({ chip, title, link }) {
  return (
    <div className="flex flex-col justify-between gap-4 border-b border-slate-200 pb-6 md:flex-row md:items-end">
      <div>
        <div className="mb-2 inline-block rounded-full bg-blue-50 px-3 py-1 text-xs font-bold uppercase tracking-wider text-blue-600">
          {chip}
        </div>
        <h2 className="font-serif text-3xl font-bold text-slate-900 md:text-4xl">{title}</h2>
      </div>

      {link && (
        <Link
          to={link}
          className="flex items-center gap-2 text-sm font-bold uppercase tracking-wider text-slate-500 transition-colors hover:text-blue-600"
        >
          View All <span className="text-lg">-&gt;</span>
        </Link>
      )}
    </div>
  );
}
