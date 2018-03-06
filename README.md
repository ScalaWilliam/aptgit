# aptgit
The way Git is supposed to be.

## Prelude: being independent
Numerous Git hosting solutions have prevented progress via the simple fact that data is exposed not via Git, but via HTTP APIs. And that updates are exposed via non-standardised webhooks.

This project concerns itself with data down to Git level. What sort of data? Take a look at the typical day-to-day workflow: issue tracker, repository hosting, continuous integration and finally deployment. All of this data belogs directly in Git rather than outside of it.

The reason is simple: it's all coupled, and the principle is: don't couple that which shouldn't be coupled; don't decouple stuff that which should be coupled.

I've faced countless problems with this coupled information being torn apart, where I'm unable to exercise the full power of Git and design tightly-wrapped workflows. The very first of these problems I faced back 12 years ago was continuously deploying website content: in order to do it, I'd need to open up a custom script that accepts webhooks, verify that I'm not being DOSsed, set up the webhook, and hope it works. It was always cumbersome but I didn't know what to do about it other than push to self-managed Git repository which would lose me all the benefits of a Git repository.

Once I began working in larger teams, I came across issue systems. In particular, JIRA, and later on, GitHub. So powerful, yet so... hard to analyse: dozens of HTTP endpoints, authentications and parsing magic to happen. Why can't I re-use my Git authentication? Why can't I simply access this as data, locally? Why do I have to be worried about whether GitHub will one day decide to remove my project, and thus kill its whole Issue history? I shouldn't be. Just like I'm not worried about the code as it's cloned locally and in multiple places, why can't the issues be as well?
